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
import java.util.Queue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.remoting.Connection;
import infra.remoting.error.ConnectionErrorException;
import infra.remoting.error.RejectedResumeException;
import infra.remoting.frame.ResumeFrameCodec;
import infra.remoting.frame.ResumeOkFrameCodec;
import infra.remoting.keepalive.KeepAliveSupport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.CharsetUtil;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.util.concurrent.Queues;

public class ServerChannelSession implements ChannelSession, ResumeStateHolder, CoreSubscriber<Long> {

  private static final Logger logger = LoggerFactory.getLogger(ServerChannelSession.class);

  final ResumableConnection resumableConnection;
  final Duration resumeSessionDuration;
  final ResumableFramesStore resumableFramesStore;
  final String session;
  final ByteBufAllocator allocator;
  final boolean cleanupStoreOnKeepAlive;

  /**
   * All incoming connections with the Resume intent are enqueued in this queue. Such an approach
   * ensure that the new connection will affect the resumption state anyhow until the previous
   * (active) connection is finally closed
   */
  final Queue<Runnable> connectionsQueue;

  volatile int wip;
  static final AtomicIntegerFieldUpdater<ServerChannelSession> WIP =
          AtomicIntegerFieldUpdater.newUpdater(ServerChannelSession.class, "wip");

  volatile Subscription s;
  static final AtomicReferenceFieldUpdater<ServerChannelSession, Subscription> S =
          AtomicReferenceFieldUpdater.newUpdater(ServerChannelSession.class, Subscription.class, "s");

  KeepAliveSupport keepAliveSupport;

  public ServerChannelSession(ByteBuf session,
          ResumableConnection resumableConnection,
          Connection initialConnection,
          ResumableFramesStore resumableFramesStore,
          Duration resumeSessionDuration,
          boolean cleanupStoreOnKeepAlive) {
    this.session = session.toString(CharsetUtil.UTF_8);
    this.allocator = initialConnection.alloc();
    this.resumeSessionDuration = resumeSessionDuration;
    this.resumableFramesStore = resumableFramesStore;
    this.cleanupStoreOnKeepAlive = cleanupStoreOnKeepAlive;
    this.resumableConnection = resumableConnection;
    this.connectionsQueue = Queues.<Runnable>unboundedMultiproducer().get();

    WIP.lazySet(this, 1);

    resumableConnection.onClose().doFinally(__ -> dispose()).subscribe();
    resumableConnection.onActiveConnectionClosed().subscribe(__ -> tryTimeoutSession());
  }

  void tryTimeoutSession() {
    keepAliveSupport.stop();

    if (logger.isDebugEnabled()) {
      logger.debug(
              "Side[server]|Session[{}]. Connection is lost. Trying to timeout the active session",
              session);
    }

    Mono.delay(resumeSessionDuration).subscribe(this);

    if (WIP.decrementAndGet(this) == 0) {
      return;
    }

    final Runnable doResumeRunnable = connectionsQueue.poll();
    if (doResumeRunnable != null) {
      doResumeRunnable.run();
    }
  }

  public void resumeWith(ByteBuf resumeFrame, Connection nextConnection) {

    if (logger.isDebugEnabled()) {
      logger.debug("Side[server]|Session[{}]. New Connection received.", session);
    }

    long remotePos = ResumeFrameCodec.firstAvailableClientPos(resumeFrame);
    long remoteImpliedPos = ResumeFrameCodec.lastReceivedServerPos(resumeFrame);

    connectionsQueue.offer(() -> doResume(remotePos, remoteImpliedPos, nextConnection));

    if (WIP.getAndIncrement(this) != 0) {
      return;
    }

    final Runnable doResumeRunnable = connectionsQueue.poll();
    if (doResumeRunnable != null) {
      doResumeRunnable.run();
    }
  }

  void doResume(long remotePos, long remoteImpliedPos, Connection nextConnection) {
    if (!tryCancelSessionTimeout()) {
      if (logger.isDebugEnabled()) {
        logger.debug(
                "Side[server]|Session[{}]. Session has already been expired. Terminating received connection",
                session);
      }
      final RejectedResumeException rejectedResumeException =
              new RejectedResumeException("resume_internal_error: Session Expired");
      nextConnection.sendErrorAndClose(rejectedResumeException);
      nextConnection.receive().subscribe();
      return;
    }

    long impliedPosition = resumableFramesStore.frameImpliedPosition();
    long position = resumableFramesStore.framePosition();

    if (logger.isDebugEnabled()) {
      logger.debug(
              "Side[server]|Session[{}]. Resume FRAME received. ServerResumeState[impliedPosition[{}], position[{}]]. ClientResumeState[remoteImpliedPosition[{}], remotePosition[{}]]",
              session,
              impliedPosition,
              position,
              remoteImpliedPos,
              remotePos);
    }

    if (remotePos <= impliedPosition && position <= remoteImpliedPos) {
      try {
        if (position != remoteImpliedPos) {
          resumableFramesStore.releaseFrames(remoteImpliedPos);
        }
        nextConnection.sendFrame(0, ResumeOkFrameCodec.encode(allocator, impliedPosition));
        if (logger.isDebugEnabled()) {
          logger.debug(
                  "Side[server]|Session[{}]. ResumeOKFrame[impliedPosition[{}]] has been sent",
                  session,
                  impliedPosition);
        }
      }
      catch (Throwable t) {
        if (logger.isDebugEnabled()) {
          logger.debug(
                  "Side[server]|Session[{}]. Exception occurred while releasing frames in the frameStore",
                  session,
                  t);
        }

        dispose();

        final RejectedResumeException rejectedResumeException =
                new RejectedResumeException(t.getMessage(), t);
        nextConnection.sendErrorAndClose(rejectedResumeException);
        nextConnection.receive().subscribe();

        return;
      }

      keepAliveSupport.start();

      if (logger.isDebugEnabled()) {
        logger.debug("Side[server]|Session[{}]. Session has been resumed successfully", session);
      }

      if (!resumableConnection.connect(nextConnection)) {
        if (logger.isDebugEnabled()) {
          logger.debug(
                  "Side[server]|Session[{}]. Session has already been expired. Terminating received connection",
                  session);
        }
        final RejectedResumeException rejectedResumeException =
                new RejectedResumeException("resume_internal_error: Session Expired");
        nextConnection.sendErrorAndClose(rejectedResumeException);
        nextConnection.receive().subscribe();

        // resumableConnection is likely to be disposed at this stage. Thus we have
        // nothing to do
      }
    }
    else {
      if (logger.isDebugEnabled()) {
        logger.debug(
                "Side[server]|Session[{}]. Mismatching remote and local state. Expected RemoteImpliedPosition[{}] to be greater or equal to the LocalPosition[{}] and RemotePosition[{}] to be less or equal to LocalImpliedPosition[{}]. Terminating received connection",
                session,
                remoteImpliedPos,
                position,
                remotePos,
                impliedPosition);
      }

      dispose();

      final RejectedResumeException rejectedResumeException =
              new RejectedResumeException(
                      String.format(
                              "resumption_pos=[ remote: { pos: %d, impliedPos: %d }, local: { pos: %d, impliedPos: %d }]",
                              remotePos, remoteImpliedPos, position, impliedPosition));
      nextConnection.sendErrorAndClose(rejectedResumeException);
      nextConnection.receive().subscribe();
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
  public void onSubscribe(Subscription s) {
    if (Operators.setOnce(S, this, s)) {
      s.request(Long.MAX_VALUE);
    }
  }

  @Override
  public void onNext(Long aLong) {
    if (!Operators.terminate(S, this)) {
      return;
    }

    resumableConnection.dispose();
  }

  @Override
  public void onComplete() { }

  @Override
  public void onError(Throwable t) { }

  public void setKeepAliveSupport(KeepAliveSupport keepAliveSupport) {
    this.keepAliveSupport = keepAliveSupport;
  }

  @Override
  public void dispose() {
    if (logger.isDebugEnabled()) {
      logger.debug("Side[server]|Session[{}]. Disposing session", session);
    }
    Operators.terminate(S, this);
    resumableConnection.dispose();
  }

  @Override
  public boolean isDisposed() {
    return resumableConnection.isDisposed();
  }
}
