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

import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.remoting.Connection;
import infra.remoting.ProtocolErrorException;
import infra.remoting.error.ConnectionErrorException;
import infra.remoting.frame.ErrorFrameCodec;
import infra.remoting.frame.FrameHeaderCodec;
import infra.remoting.internal.UnboundedProcessor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.CharsetUtil;
import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.core.publisher.Sinks;

public class ResumableConnection extends Flux<ByteBuf> implements Connection, Subscription {

  static final Logger logger = LoggerFactory.getLogger(ResumableConnection.class);

  final String side;
  final String session;
  final SocketAddress remoteAddress;

  final ResumableFramesStore resumableFramesStore;

  final UnboundedProcessor savableFramesSender;
  final Sinks.Empty<Void> onQueueClose;
  final Sinks.Empty<Void> onLastConnectionClose;
  final Sinks.Many<Integer> onConnectionClosedSink;

  CoreSubscriber<? super ByteBuf> receiveSubscriber;
  FrameReceivingSubscriber activeReceivingSubscriber;

  volatile int state;
  static final AtomicIntegerFieldUpdater<ResumableConnection> STATE =
          AtomicIntegerFieldUpdater.newUpdater(ResumableConnection.class, "state");

  volatile Connection activeConnection;
  static final AtomicReferenceFieldUpdater<ResumableConnection, Connection>
          ACTIVE_CONNECTION =
          AtomicReferenceFieldUpdater.newUpdater(
                  ResumableConnection.class, Connection.class, "activeConnection");

  int connectionIndex = 0;

  public ResumableConnection(String side, ByteBuf session, Connection initialConnection, ResumableFramesStore store) {
    this.side = side;
    this.session = session.toString(CharsetUtil.UTF_8);
    this.onConnectionClosedSink = Sinks.unsafe().many().unicast().onBackpressureBuffer();
    this.resumableFramesStore = store;
    this.onQueueClose = Sinks.unsafe().empty();
    this.onLastConnectionClose = Sinks.unsafe().empty();
    this.savableFramesSender = new UnboundedProcessor(onQueueClose::tryEmitEmpty);
    this.remoteAddress = initialConnection.remoteAddress();

    store.saveFrames(savableFramesSender).subscribe();

    ACTIVE_CONNECTION.lazySet(this, initialConnection);
  }

  public boolean connect(Connection nextConnection) {
    final Connection activeConnection = this.activeConnection;
    if (activeConnection != DisposedConnection.INSTANCE
            && ACTIVE_CONNECTION.compareAndSet(this, activeConnection, nextConnection)) {

      if (!activeConnection.isDisposed()) {
        activeConnection.sendErrorAndClose(
                new ConnectionErrorException("Connection unexpectedly replaced"));
      }

      initConnection(nextConnection);

      return true;
    }
    else {
      return false;
    }
  }

  private void initConnection(Connection nextConnection) {
    final int nextConnectionIndex = this.connectionIndex + 1;
    final var frameReceivingSubscriber = new FrameReceivingSubscriber(side, resumableFramesStore, receiveSubscriber);

    this.connectionIndex = nextConnectionIndex;
    this.activeReceivingSubscriber = frameReceivingSubscriber;

    if (logger.isDebugEnabled()) {
      logger.debug("Side[{}]|Session[{}]|Connection[{}]. Connecting", side, session, connectionIndex);
    }

    final Disposable resumeStreamSubscription = resumableFramesStore
            .resumeStream()
            .subscribe(f -> nextConnection.sendFrame(FrameHeaderCodec.streamId(f), f),
                    t -> {
                      dispose(nextConnection, t);
                      nextConnection.sendErrorAndClose(new ConnectionErrorException(t.getMessage(), t));
                    },
                    () -> {
                      final ConnectionErrorException e =
                              new ConnectionErrorException("Connection Closed Unexpectedly");
                      dispose(nextConnection, e);
                      nextConnection.sendErrorAndClose(e);
                    });
    nextConnection.receive().subscribe(frameReceivingSubscriber);
    nextConnection.onClose().doFinally(__ -> {
              frameReceivingSubscriber.dispose();
              resumeStreamSubscription.dispose();
              if (logger.isDebugEnabled()) {
                logger.debug("Side[{}]|Session[{}]|Connection[{}]. Disconnected", side, session, connectionIndex);
              }
              Sinks.EmitResult result = onConnectionClosedSink.tryEmitNext(nextConnectionIndex);
              if (!result.equals(Sinks.EmitResult.OK)) {
                logger.error("Side[{}]|Session[{}]|Connection[{}]. Failed to notify session of closed connection: {}",
                        side, session, connectionIndex, result);
              }
            })
            .subscribe();
  }

  public void disconnect() {
    final Connection activeConnection = this.activeConnection;
    if (activeConnection != DisposedConnection.INSTANCE && !activeConnection.isDisposed()) {
      activeConnection.dispose();
    }
  }

  @Override
  public void sendFrame(int streamId, ByteBuf frame) {
    if (streamId == 0) {
      savableFramesSender.tryEmitPrioritized(frame);
    }
    else {
      savableFramesSender.tryEmitNormal(frame);
    }
  }

  /**
   * Publisher for a sequence of integers starting at 1, with each next number emitted when the
   * currently active connection is closed and should be resumed. The Publisher never emits an error
   * and completes when the connection is disposed and not resumed.
   */
  Flux<Integer> onActiveConnectionClosed() {
    return onConnectionClosedSink.asFlux();
  }

  @Override
  public void sendErrorAndClose(ProtocolErrorException exception) {
    final Connection activeConnection =
            ACTIVE_CONNECTION.getAndSet(this, DisposedConnection.INSTANCE);
    if (activeConnection == DisposedConnection.INSTANCE) {
      return;
    }

    savableFramesSender.tryEmitFinal(
            ErrorFrameCodec.encode(activeConnection.alloc(), 0, exception));

    activeConnection.onClose().subscribe(null,
            t -> {
              onConnectionClosedSink.tryEmitComplete();
              onLastConnectionClose.tryEmitEmpty();
            },
            () -> {
              onConnectionClosedSink.tryEmitComplete();

              final Throwable cause = exception.getCause();
              if (cause == null) {
                onLastConnectionClose.tryEmitEmpty();
              }
              else {
                onLastConnectionClose.tryEmitError(cause);
              }
            });
  }

  @Override
  public Flux<ByteBuf> receive() {
    return this;
  }

  @Override
  public ByteBufAllocator alloc() {
    return activeConnection.alloc();
  }

  @Override
  public Mono<Void> onClose() {
    return Mono.whenDelayError(
            onQueueClose.asMono(), resumableFramesStore.onClose(), onLastConnectionClose.asMono());
  }

  @Override
  public void dispose() {
    final Connection activeConnection = ACTIVE_CONNECTION.getAndSet(this, DisposedConnection.INSTANCE);
    if (activeConnection == DisposedConnection.INSTANCE) {
      return;
    }
    savableFramesSender.onComplete();
    activeConnection.onClose().subscribe(null,
            t -> {
              onConnectionClosedSink.tryEmitComplete();
              onLastConnectionClose.tryEmitEmpty();
            },
            () -> {
              onConnectionClosedSink.tryEmitComplete();
              onLastConnectionClose.tryEmitEmpty();
            });
  }

  void dispose(Connection nextConnection, @Nullable Throwable e) {
    final Connection activeConnection = ACTIVE_CONNECTION.getAndSet(this, DisposedConnection.INSTANCE);
    if (activeConnection == DisposedConnection.INSTANCE) {
      return;
    }
    savableFramesSender.onComplete();
    nextConnection.onClose().subscribe(null,
            t -> {
              if (e != null) {
                onLastConnectionClose.tryEmitError(e);
              }
              else {
                onLastConnectionClose.tryEmitEmpty();
              }
              onConnectionClosedSink.tryEmitComplete();
            },
            () -> {
              if (e != null) {
                onLastConnectionClose.tryEmitError(e);
              }
              else {
                onLastConnectionClose.tryEmitEmpty();
              }
              onConnectionClosedSink.tryEmitComplete();
            });
  }

  @Override
  @SuppressWarnings("ConstantConditions")
  public boolean isDisposed() {
    return onQueueClose.scan(Scannable.Attr.TERMINATED)
            || onQueueClose.scan(Scannable.Attr.CANCELLED);
  }

  @Override
  public SocketAddress remoteAddress() {
    return remoteAddress;
  }

  @Override
  public void request(long n) {
    if (state == 1 && STATE.compareAndSet(this, 1, 2)) {
      // happens for the very first time with the initial connection
      initConnection(this.activeConnection);
    }
  }

  @Override
  public void cancel() {
    dispose();
  }

  @Override
  public void subscribe(CoreSubscriber<? super ByteBuf> receiverSubscriber) {
    if (state == 0 && STATE.compareAndSet(this, 0, 1)) {
      receiveSubscriber = receiverSubscriber;
      receiverSubscriber.onSubscribe(this);
    }
  }

  static boolean isResumableFrame(ByteBuf frame) {
    return FrameHeaderCodec.streamId(frame) != 0;
  }

  @Override
  public String toString() {
    return "ResumableConnection{side='%s', session='%s', remoteAddress=%s, state=%d, activeConnection=%s, connectionIndex=%d}"
            .formatted(side, session, remoteAddress, state, activeConnection, connectionIndex);
  }

  private static final class DisposedConnection implements Connection {

    static final DisposedConnection INSTANCE = new DisposedConnection();

    private DisposedConnection() {
    }

    @Override
    public void dispose() { }

    @Override
    public Mono<Void> onClose() {
      return Mono.never();
    }

    @Override
    public void sendFrame(int streamId, ByteBuf frame) {
    }

    @Override
    public Flux<ByteBuf> receive() {
      return Flux.never();
    }

    @Override
    public void sendErrorAndClose(ProtocolErrorException e) {
    }

    @Override
    public ByteBufAllocator alloc() {
      return ByteBufAllocator.DEFAULT;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public SocketAddress remoteAddress() {
      return null;
    }
  }

  private static final class FrameReceivingSubscriber implements CoreSubscriber<ByteBuf>, Disposable {

    private final ResumableFramesStore resumableFramesStore;
    private final CoreSubscriber<? super ByteBuf> actual;
    private final String tag;

    volatile Subscription s;
    static final AtomicReferenceFieldUpdater<FrameReceivingSubscriber, Subscription> S =
            AtomicReferenceFieldUpdater.newUpdater(
                    FrameReceivingSubscriber.class, Subscription.class, "s");

    boolean cancelled;

    private FrameReceivingSubscriber(String tag, ResumableFramesStore store, CoreSubscriber<? super ByteBuf> actual) {
      this.tag = tag;
      this.resumableFramesStore = store;
      this.actual = actual;
    }

    @Override
    public void onSubscribe(Subscription s) {
      if (Operators.setOnce(S, this, s)) {
        s.request(Long.MAX_VALUE);
      }
    }

    @Override
    public void onNext(ByteBuf frame) {
      if (cancelled || s == Operators.cancelledSubscription()) {
        return;
      }

      if (isResumableFrame(frame)) {
        if (resumableFramesStore.resumableFrameReceived(frame)) {
          actual.onNext(frame);
        }
        return;
      }

      actual.onNext(frame);
    }

    @Override
    public void onError(Throwable t) {
      Operators.set(S, this, Operators.cancelledSubscription());
    }

    @Override
    public void onComplete() {
      Operators.set(S, this, Operators.cancelledSubscription());
    }

    @Override
    public void dispose() {
      cancelled = true;
      Operators.terminate(S, this);
    }

    @Override
    public boolean isDisposed() {
      return cancelled || s == Operators.cancelledSubscription();
    }
  }
}
