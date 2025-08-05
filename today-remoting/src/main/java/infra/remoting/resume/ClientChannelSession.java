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
import infra.remoting.DuplexConnection;
import infra.remoting.error.ConnectionErrorException;
import infra.remoting.error.Exceptions;
import infra.remoting.error.RejectedResumeException;
import infra.remoting.frame.FrameHeaderCodec;
import infra.remoting.frame.FrameType;
import infra.remoting.frame.ResumeFrameCodec;
import infra.remoting.frame.ResumeOkFrameCodec;
import infra.remoting.keepalive.KeepAliveSupport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.CharsetUtil;
import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.util.function.Tuple2;
import reactor.util.retry.Retry;

public class ClientChannelSession implements ChannelSession, ResumeStateHolder,
        CoreSubscriber<Tuple2<ByteBuf, DuplexConnection>> {

  private static final Logger logger = LoggerFactory.getLogger(ClientChannelSession.class);

  final ResumableDuplexConnection resumableConnection;
  final Mono<Tuple2<ByteBuf, DuplexConnection>> connectionFactory;
  final ResumableFramesStore resumableFramesStore;

  final ByteBufAllocator allocator;
  final Duration resumeSessionDuration;
  final Retry retry;
  final boolean cleanupStoreOnKeepAlive;
  final ByteBuf resumeToken;
  final String session;
  final Disposable reconnectDisposable;

  volatile Subscription s;
  static final AtomicReferenceFieldUpdater<ClientChannelSession, Subscription> S =
          AtomicReferenceFieldUpdater.newUpdater(ClientChannelSession.class, Subscription.class, "s");

  KeepAliveSupport keepAliveSupport;

  public ClientChannelSession(
          ByteBuf resumeToken,
          ResumableDuplexConnection resumableDuplexConnection,
          Mono<DuplexConnection> connectionFactory,
          Function<DuplexConnection, Mono<Tuple2<ByteBuf, DuplexConnection>>> connectionTransformer,
          ResumableFramesStore resumableFramesStore,
          Duration resumeSessionDuration,
          Retry retry,
          boolean cleanupStoreOnKeepAlive) {
    this.resumeToken = resumeToken;
    this.session = resumeToken.toString(CharsetUtil.UTF_8);
    this.connectionFactory =
            connectionFactory
                    .doOnDiscard(
                            DuplexConnection.class,
                            c -> {
                              final ConnectionErrorException connectionErrorException =
                                      new ConnectionErrorException("resumption_server=[Session Expired]");
                              c.sendErrorAndClose(connectionErrorException);
                              c.receive().subscribe();
                            })
                    .flatMap(
                            dc -> {
                              final long impliedPosition = resumableFramesStore.frameImpliedPosition();
                              final long position = resumableFramesStore.framePosition();
                              dc.sendFrame(
                                      0,
                                      ResumeFrameCodec.encode(
                                              dc.alloc(),
                                              resumeToken.retain(),
                                              // server uses this to release its cache
                                              impliedPosition, //  observed on the client side
                                              // server uses this to check whether there is no mismatch
                                              position //  sent from the client sent
                                      ));

                              if (logger.isDebugEnabled()) {
                                logger.debug(
                                        "Side[client]|Session[{}]. ResumeFrame[impliedPosition[{}], position[{}]] has been sent.",
                                        session,
                                        impliedPosition,
                                        position);
                              }

                              return connectionTransformer.apply(dc);
                            })
                    .doOnDiscard(Tuple2.class, this::tryReestablishSession);
    this.resumableFramesStore = resumableFramesStore;
    this.allocator = resumableDuplexConnection.alloc();
    this.resumeSessionDuration = resumeSessionDuration;
    this.retry = retry;
    this.cleanupStoreOnKeepAlive = cleanupStoreOnKeepAlive;
    this.resumableConnection = resumableDuplexConnection;

    resumableDuplexConnection.onClose().doFinally(__ -> dispose()).subscribe();

    this.reconnectDisposable =
            resumableDuplexConnection.onActiveConnectionClosed().subscribe(this::reconnect);
  }

  void reconnect(int index) {
    if (this.s == Operators.cancelledSubscription()) {
      if (logger.isDebugEnabled()) {
        logger.debug(
                "Side[client]|Session[{}]. Connection[{}] is lost. Reconnecting rejected since session is closed",
                session,
                index);
      }
      return;
    }

    keepAliveSupport.stop();
    if (logger.isDebugEnabled()) {
      logger.debug(
              "Side[client]|Session[{}]. Connection[{}] is lost. Reconnecting to resume...",
              session,
              index);
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

  void tryReestablishSession(Tuple2<ByteBuf, DuplexConnection> tuple2) {
    if (logger.isDebugEnabled()) {
      logger.debug("Active subscription is canceled {}", s == Operators.cancelledSubscription());
    }
    ByteBuf shouldBeResumeOKFrame = tuple2.getT1();
    DuplexConnection nextDuplexConnection = tuple2.getT2();

    final int streamId = FrameHeaderCodec.streamId(shouldBeResumeOKFrame);
    if (streamId != 0) {
      if (logger.isDebugEnabled()) {
        logger.debug(
                "Side[client]|Session[{}]. Illegal first frame received. RESUME_OK frame must be received before any others. Terminating received connection",
                session);
      }
      final ConnectionErrorException connectionErrorException =
              new ConnectionErrorException("RESUME_OK frame must be received before any others");
      resumableConnection.dispose(nextDuplexConnection, connectionErrorException);
      nextDuplexConnection.sendErrorAndClose(connectionErrorException);
      nextDuplexConnection.receive().subscribe();

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
        logger.debug(
                "Side[client]|Session[{}]. ResumeOK FRAME received. ServerResumeState[remoteImpliedPosition[{}]]. ClientResumeState[impliedPosition[{}], position[{}]]",
                session,
                remoteImpliedPos,
                impliedPosition,
                position);
      }
      if (position <= remoteImpliedPos) {
        try {
          if (position != remoteImpliedPos) {
            resumableFramesStore.releaseFrames(remoteImpliedPos);
          }
        }
        catch (IllegalStateException e) {
          if (logger.isDebugEnabled()) {
            logger.debug(
                    "Side[client]|Session[{}]. Exception occurred while releasing frames in the frameStore",
                    session,
                    e);
          }
          final ConnectionErrorException t = new ConnectionErrorException(e.getMessage(), e);

          resumableConnection.dispose(nextDuplexConnection, t);

          nextDuplexConnection.sendErrorAndClose(t);
          nextDuplexConnection.receive().subscribe();

          return;
        }

        if (!tryCancelSessionTimeout()) {
          if (logger.isDebugEnabled()) {
            logger.debug(
                    "Side[client]|Session[{}]. Session has already been expired. Terminating received connection",
                    session);
          }
          final ConnectionErrorException connectionErrorException =
                  new ConnectionErrorException("resumption_server=[Session Expired]");
          nextDuplexConnection.sendErrorAndClose(connectionErrorException);
          nextDuplexConnection.receive().subscribe();
          return;
        }

        keepAliveSupport.start();

        if (logger.isDebugEnabled()) {
          logger.debug("Side[client]|Session[{}]. Session has been resumed successfully", session);
        }

        if (!resumableConnection.connect(nextDuplexConnection)) {
          if (logger.isDebugEnabled()) {
            logger.debug(
                    "Side[client]|Session[{}]. Session has already been expired. Terminating received connection",
                    session);
          }
          final ConnectionErrorException connectionErrorException =
                  new ConnectionErrorException("resumption_server_pos=[Session Expired]");
          nextDuplexConnection.sendErrorAndClose(connectionErrorException);
          nextDuplexConnection.receive().subscribe();
          // no need to do anything since connection resumable connection is liklly to
          // be disposed
        }
      }
      else {
        if (logger.isDebugEnabled()) {
          logger.debug(
                  "Side[client]|Session[{}]. Mismatching remote and local state. Expected RemoteImpliedPosition[{}] to be greater or equal to the LocalPosition[{}]. Terminating received connection",
                  session,
                  remoteImpliedPos,
                  position);
        }
        final ConnectionErrorException connectionErrorException =
                new ConnectionErrorException("resumption_server_pos=[" + remoteImpliedPos + "]");

        resumableConnection.dispose(nextDuplexConnection, connectionErrorException);

        nextDuplexConnection.sendErrorAndClose(connectionErrorException);
        nextDuplexConnection.receive().subscribe();
      }
    }
    else if (frameType == FrameType.ERROR) {
      final RuntimeException exception = Exceptions.from(0, shouldBeResumeOKFrame);
      if (logger.isDebugEnabled()) {
        logger.debug(
                "Side[client]|Session[{}]. Received error frame. Terminating received connection",
                session,
                exception);
      }
      if (exception instanceof RejectedResumeException) {
        resumableConnection.dispose(nextDuplexConnection, exception);
        nextDuplexConnection.dispose();
        nextDuplexConnection.receive().subscribe();
        return;
      }

      nextDuplexConnection.dispose();
      nextDuplexConnection.receive().subscribe();
      throw exception; // assume retryable exception
    }
    else {
      if (logger.isDebugEnabled()) {
        logger.debug(
                "Side[client]|Session[{}]. Illegal first frame received. RESUME_OK frame must be received before any others. Terminating received connection",
                session);
      }
      final ConnectionErrorException connectionErrorException =
              new ConnectionErrorException("RESUME_OK frame must be received before any others");

      resumableConnection.dispose(nextDuplexConnection, connectionErrorException);

      nextDuplexConnection.sendErrorAndClose(connectionErrorException);
      nextDuplexConnection.receive().subscribe();

      // no need to do anything since remote server rejected our connection completely
    }
  }

  boolean tryCancelSessionTimeout() {
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
  public void onNext(Tuple2<ByteBuf, DuplexConnection> objects) { }

  @Override
  public void onError(Throwable t) {
    if (!Operators.terminate(S, this)) {
      Operators.onErrorDropped(t, currentContext());
    }

    resumableConnection.dispose();
  }

  @Override
  public void onComplete() { }

  public void setKeepAliveSupport(KeepAliveSupport keepAliveSupport) {
    this.keepAliveSupport = keepAliveSupport;
  }
}
