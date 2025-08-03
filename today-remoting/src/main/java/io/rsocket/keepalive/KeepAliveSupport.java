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

package io.rsocket.keepalive;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.rsocket.frame.KeepAliveFrameCodec;
import io.rsocket.resume.ResumeStateHolder;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public abstract class KeepAliveSupport implements KeepAliveFramesAcceptor {

  final ByteBufAllocator allocator;
  final Scheduler scheduler;
  final Duration keepAliveInterval;
  final Duration keepAliveTimeout;
  final long keepAliveTimeoutMillis;

  volatile int state;
  static final AtomicIntegerFieldUpdater<KeepAliveSupport> STATE =
          AtomicIntegerFieldUpdater.newUpdater(KeepAliveSupport.class, "state");

  static final int STOPPED_STATE = 0;
  static final int STARTING_STATE = 1;
  static final int STARTED_STATE = 2;
  static final int DISPOSED_STATE = -1;

  volatile Consumer<KeepAlive> onTimeout;
  volatile Consumer<ByteBuf> onFrameSent;

  Disposable ticksDisposable;

  volatile ResumeStateHolder resumeStateHolder;
  volatile long lastReceivedMillis;

  private KeepAliveSupport(
          ByteBufAllocator allocator, int keepAliveInterval, int keepAliveTimeout) {
    this.allocator = allocator;
    this.scheduler = Schedulers.parallel();
    this.keepAliveInterval = Duration.ofMillis(keepAliveInterval);
    this.keepAliveTimeout = Duration.ofMillis(keepAliveTimeout);
    this.keepAliveTimeoutMillis = keepAliveTimeout;
  }

  public KeepAliveSupport start() {
    if (this.state == STOPPED_STATE && STATE.compareAndSet(this, STOPPED_STATE, STARTING_STATE)) {
      this.lastReceivedMillis = scheduler.now(TimeUnit.MILLISECONDS);

      final Disposable disposable =
              Flux.interval(keepAliveInterval, scheduler).subscribe(v -> onIntervalTick());
      this.ticksDisposable = disposable;

      if (this.state != STARTING_STATE
              || !STATE.compareAndSet(this, STARTING_STATE, STARTED_STATE)) {
        disposable.dispose();
      }
    }
    return this;
  }

  public void stop() {
    terminate(STOPPED_STATE);
  }

  @Override
  public void receive(ByteBuf keepAliveFrame) {
    this.lastReceivedMillis = scheduler.now(TimeUnit.MILLISECONDS);
    if (resumeStateHolder != null) {
      final long remoteLastReceivedPos = KeepAliveFrameCodec.lastPosition(keepAliveFrame);
      resumeStateHolder.onImpliedPosition(remoteLastReceivedPos);
    }
    if (KeepAliveFrameCodec.respondFlag(keepAliveFrame)) {
      long localLastReceivedPos = localLastReceivedPosition();
      send(KeepAliveFrameCodec.encode(
              allocator,
              false,
              localLastReceivedPos,
              KeepAliveFrameCodec.data(keepAliveFrame).retain()));
    }
  }

  public KeepAliveSupport resumeState(ResumeStateHolder resumeStateHolder) {
    this.resumeStateHolder = resumeStateHolder;
    return this;
  }

  public KeepAliveSupport onSendKeepAliveFrame(Consumer<ByteBuf> onFrameSent) {
    this.onFrameSent = onFrameSent;
    return this;
  }

  public KeepAliveSupport onTimeout(Consumer<KeepAlive> onTimeout) {
    this.onTimeout = onTimeout;
    return this;
  }

  @Override
  public void dispose() {
    terminate(DISPOSED_STATE);
  }

  @Override
  public boolean isDisposed() {
    return ticksDisposable.isDisposed();
  }

  abstract void onIntervalTick();

  void send(ByteBuf frame) {
    if (onFrameSent != null) {
      onFrameSent.accept(frame);
    }
  }

  void tryTimeout() {
    long now = scheduler.now(TimeUnit.MILLISECONDS);
    if (now - lastReceivedMillis >= keepAliveTimeoutMillis) {
      if (onTimeout != null) {
        onTimeout.accept(new KeepAlive(keepAliveInterval, keepAliveTimeout));
      }
      stop();
    }
  }

  void terminate(int terminationState) {
    for (; ; ) {
      final int state = this.state;

      if (state == STOPPED_STATE || state == DISPOSED_STATE) {
        return;
      }

      final Disposable disposable = this.ticksDisposable;
      if (STATE.compareAndSet(this, state, terminationState)) {
        disposable.dispose();
        return;
      }
    }
  }

  long localLastReceivedPosition() {
    return resumeStateHolder != null ? resumeStateHolder.impliedPosition() : 0;
  }

  public static final class ClientKeepAliveSupport extends KeepAliveSupport {

    public ClientKeepAliveSupport(
            ByteBufAllocator allocator, int keepAliveInterval, int keepAliveTimeout) {
      super(allocator, keepAliveInterval, keepAliveTimeout);
    }

    @Override
    void onIntervalTick() {
      tryTimeout();
      send(KeepAliveFrameCodec.encode(allocator, true, localLastReceivedPosition(), Unpooled.EMPTY_BUFFER));
    }
  }

  public static final class KeepAlive {
    private final Duration tickPeriod;
    private final Duration timeoutMillis;

    public KeepAlive(Duration tickPeriod, Duration timeoutMillis) {
      this.tickPeriod = tickPeriod;
      this.timeoutMillis = timeoutMillis;
    }

    public Duration getTickPeriod() {
      return tickPeriod;
    }

    public Duration getTimeout() {
      return timeoutMillis;
    }
  }
}
