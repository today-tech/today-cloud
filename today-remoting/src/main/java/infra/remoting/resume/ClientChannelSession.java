/*
 * Copyright 2021 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package infra.remoting.resume;

import org.reactivestreams.Subscription;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Function;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.remoting.Connection;
import infra.remoting.error.ConnectionErrorException;
import infra.remoting.error.Exceptions;
import infra.remoting.error.RejectedResumeException;
import infra.remoting.frame.FrameHeaderCodec;
import infra.remoting.frame.FrameType;
import infra.remoting.frame.ResumeFrameCodec;
import infra.remoting.frame.ResumeOkFrameCodec;
import infra.remoting.keepalive.KeepAliveSupport;
import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;
import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.util.function.Tuple2;
import reactor.util.retry.Retry;

public class ClientChannelSession implements ChannelSession, ResumeStateHolder, CoreSubscriber<Tuple2<ByteBuf, Connection>> {

  private static final Logger logger = LoggerFactory.getLogger(ClientChannelSession.class);

  private final ResumableConnection resumableConnection;
  private final Mono<Tuple2<ByteBuf, Connection>> connectionFactory;
  private final ResumableFramesStore resumableFramesStore;

  private final Duration resumeSessionDuration;
  private final Retry retry;
  private final boolean cleanupStoreOnKeepAlive;
  private final ByteBuf resumeToken;
  private final String session;
  private final Disposable reconnectDisposable;

  volatile Subscription s;
  static final AtomicReferenceFieldUpdater<ClientChannelSession, Subscription> S =
          AtomicReferenceFieldUpdater.newUpdater(ClientChannelSession.class, Subscription.class, "s");

  private KeepAliveSupport keepAliveSupport;

  public ClientChannelSession(ByteBuf resumeToken, ResumableConnection resumableConnection,
          Mono<Connection> connectionFactory, Function<Connection, Mono<Tuple2<ByteBuf, Connection>>> connectionTransformer,
          ResumableFramesStore resumableFramesStore, Duration resumeSessionDuration, Retry retry, boolean cleanupStoreOnKeepAlive) {
    this.retry = retry;
    this.session = resumeToken.toString(CharsetUtil.UTF_8);
    this.resumeToken = resumeToken;
    this.resumableConnection = resumableConnection;
    this.resumableFramesStore = resumableFramesStore;
    this.resumeSessionDuration = resumeSessionDuration;
    this.cleanupStoreOnKeepAlive = cleanupStoreOnKeepAlive;

    this.connectionFactory = connectionFactory
            .doOnDiscard(Connection.class, c -> {
              final ConnectionErrorException connectionErrorException =
                      new ConnectionErrorException("resumption_server=[Session Expired]");
              c.sendErrorAndClose(connectionErrorException);
              c.receive().subscribe();
            })
            .flatMap(dc -> {
              final long impliedPosition = resumableFramesStore.frameImpliedPosition();
              final long position = resumableFramesStore.framePosition();
              dc.sendFrame(0, ResumeFrameCodec.encode(dc.alloc(),
                      resumeToken.retain(),
                      // server uses this to release its cache
                      impliedPosition, //  observed on the client side
                      // server uses this to check whether there is no mismatch
                      position //  sent from the client sent
              ));

              if (logger.isDebugEnabled()) {
                logger.debug("Side[client]|Session[{}]. ResumeFrame[impliedPosition[{}], position[{}]] has been sent.",
                        session, impliedPosition, position);
              }

              return connectionTransformer.apply(dc);
            })
            .doOnDiscard(Tuple2.class, this::tryReestablishSession);

    resumableConnection.onClose().doFinally(__ -> dispose()).subscribe();
    this.reconnectDisposable = resumableConnection.onActiveConnectionClosed().subscribe(this::reconnect);
  }

  private void reconnect(int index) {
    if (this.s == Operators.cancelledSubscription()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Side[client]|Session[{}]. Connection[{}] is lost. Reconnecting rejected since session is closed", session, index);
      }
      return;
    }

    keepAliveSupport.stop();
    if (logger.isDebugEnabled()) {
      logger.debug("Side[client]|Session[{}]. Connection[{}] is lost. Reconnecting to resume...", session, index);
    }
    connectionFactory
            .doOnNext(this::tryReestablishSession)
            .retryWhen(retry)
            .timeout(resumeSessionDuration)
            .subscribe(this);
  }

  @Override
  public long impliedPosition() {
    return resumableFramesStore.frameImpliedPosition();
  }

  @Override
  public void onImpliedPosition(long remoteImpliedPos) {
    if (cleanupStoreOnKeepAlive) {
      try {
        resumableFramesStore.releaseFrames(remoteImpliedPos);
      }
      catch (Throwable e) {
        resumableConnection.sendErrorAndClose(new ConnectionErrorException(e.getMessage(), e));
      }
    }
  }

  @Override
  public void dispose() {
    if (logger.isDebugEnabled()) {
      logger.debug("Side[client]|Session[{}]. Disposing", session);
    }

    boolean result = Operators.terminate(S, this);

    if (logger.isDebugEnabled()) {
      logger.debug("Side[client]|Session[{}]. Sessions[isDisposed={}]", session, result);
    }

    reconnectDisposable.dispose();
    resumableConnection.dispose();
    // frame store is disposed by resumable connection
    // resumableFramesStore.dispose();

    if (resumeToken.refCnt() > 0) {
      resumeToken.release();
    }
  }

  @Override
  public boolean isDisposed() {
    return resumableConnection.isDisposed();
  }

  private void tryReestablishSession(Tuple2<ByteBuf, Connection> tuple2) {
    if (logger.isDebugEnabled()) {
      logger.debug("Active subscription is canceled {}", s == Operators.cancelledSubscription());
    }
    ByteBuf shouldBeResumeOKFrame = tuple2.getT1();
    Connection nextConnection = tuple2.getT2();

    final int streamId = FrameHeaderCodec.streamId(shouldBeResumeOKFrame);
    if (streamId != 0) {
      if (logger.isDebugEnabled()) {
        logger.debug("Side[client]|Session[{}]. Illegal first frame received. RESUME_OK frame must be received before any others. Terminating received connection", session);
      }
      final var connectionErrorException = new ConnectionErrorException("RESUME_OK frame must be received before any others");
      resumableConnection.dispose(nextConnection, connectionErrorException);
      nextConnection.sendErrorAndClose(connectionErrorException);
      nextConnection.receive().subscribe();

      throw connectionErrorException; // throw to retry connection again
    }

    final FrameType frameType = FrameHeaderCodec.nativeFrameType(shouldBeResumeOKFrame);
    if (frameType == FrameType.RESUME_OK) {
      // how  many frames the server has received from the client
      // so the client can release cached frames by this point
      long remoteImpliedPos = ResumeOkFrameCodec.lastReceivedClientPos(shouldBeResumeOKFrame);
      // what was the last notification from the server about number of frames being
      // observed
      final long position = resumableFramesStore.framePosition();
      final long impliedPosition = resumableFramesStore.frameImpliedPosition();
      if (logger.isDebugEnabled()) {
        logger.debug("Side[client]|Session[{}]. ResumeOK FRAME received. ServerResumeState[remoteImpliedPosition[{}]]. ClientResumeState[impliedPosition[{}], position[{}]]",
                session, remoteImpliedPos, impliedPosition, position);
      }
      if (position <= remoteImpliedPos) {
        try {
          if (position != remoteImpliedPos) {
            resumableFramesStore.releaseFrames(remoteImpliedPos);
          }
        }
        catch (IllegalStateException e) {
          if (logger.isDebugEnabled()) {
            logger.debug("Side[client]|Session[{}]. Exception occurred while releasing frames in the frameStore", session, e);
          }
          final ConnectionErrorException t = new ConnectionErrorException(e.getMessage(), e);

          resumableConnection.dispose(nextConnection, t);

          nextConnection.sendErrorAndClose(t);
          nextConnection.receive().subscribe();

          return;
        }

        if (!tryCancelSessionTimeout()) {
          if (logger.isDebugEnabled()) {
            logger.debug("Side[client]|Session[{}]. Session has already been expired. Terminating received connection", session);
          }
          final ConnectionErrorException connectionErrorException = new ConnectionErrorException("resumption_server=[Session Expired]");
          nextConnection.sendErrorAndClose(connectionErrorException);
          nextConnection.receive().subscribe();
          return;
        }

        keepAliveSupport.start();

        if (logger.isDebugEnabled()) {
          logger.debug("Side[client]|Session[{}]. Session has been resumed successfully", session);
        }

        if (!resumableConnection.connect(nextConnection)) {
          if (logger.isDebugEnabled()) {
            logger.debug("Side[client]|Session[{}]. Session has already been expired. Terminating received connection", session);
          }
          final ConnectionErrorException connectionErrorException =
                  new ConnectionErrorException("resumption_server_pos=[Session Expired]");
          nextConnection.sendErrorAndClose(connectionErrorException);
          nextConnection.receive().subscribe();
          // no need to do anything since connection resumable connection is liklly to
          // be disposed
        }
      }
      else {
        if (logger.isDebugEnabled()) {
          logger.debug(
                  "Side[client]|Session[{}]. Mismatching remote and local state. Expected RemoteImpliedPosition[{}] to be greater or equal to the LocalPosition[{}]. Terminating received connection",
                  session, remoteImpliedPos, position);
        }
        final ConnectionErrorException connectionErrorException =
                new ConnectionErrorException("resumption_server_pos=[" + remoteImpliedPos + "]");

        resumableConnection.dispose(nextConnection, connectionErrorException);

        nextConnection.sendErrorAndClose(connectionErrorException);
        nextConnection.receive().subscribe();
      }
    }
    else if (frameType == FrameType.ERROR) {
      final RuntimeException exception = Exceptions.from(0, shouldBeResumeOKFrame);
      if (logger.isDebugEnabled()) {
        logger.debug("Side[client]|Session[{}]. Received error frame. Terminating received connection", session, exception);
      }
      if (exception instanceof RejectedResumeException) {
        resumableConnection.dispose(nextConnection, exception);
        nextConnection.dispose();
        nextConnection.receive().subscribe();
        return;
      }

      nextConnection.dispose();
      nextConnection.receive().subscribe();
      throw exception; // assume retryable exception
    }
    else {
      if (logger.isDebugEnabled()) {
        logger.debug("Side[client]|Session[{}]. Illegal first frame received. RESUME_OK frame must be received before any others. Terminating received connection", session);
      }
      final var connectionErrorException = new ConnectionErrorException("RESUME_OK frame must be received before any others");
      resumableConnection.dispose(nextConnection, connectionErrorException);

      nextConnection.sendErrorAndClose(connectionErrorException);
      nextConnection.receive().subscribe();

      // no need to do anything since remote server rejected our connection completely
    }
  }

  private boolean tryCancelSessionTimeout() {
    for (; ; ) {
      final Subscription subscription = this.s;

      if (subscription == Operators.cancelledSubscription()) {
        return false;
      }

      if (S.compareAndSet(this, subscription, null)) {
        subscription.cancel();
        return true;
      }
    }
  }

  @Override
  public void onSubscribe(Subscription s) {
    if (Operators.setOnce(S, this, s)) {
      s.request(Long.MAX_VALUE);
    }
  }

  @Override
  public void onNext(Tuple2<ByteBuf, Connection> objects) {
  }

  @Override
  public void onError(Throwable t) {
    if (!Operators.terminate(S, this)) {
      Operators.onErrorDropped(t, currentContext());
    }

    resumableConnection.dispose();
  }

  @Override
  public void onComplete() {
  }

  @Override
  public void setKeepAliveSupport(KeepAliveSupport keepAliveSupport) {
    this.keepAliveSupport = keepAliveSupport;
  }

}
