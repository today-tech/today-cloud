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

package io.rsocket.lb;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import io.netty.util.ReferenceCountUtil;
import io.rsocket.Channel;
import io.rsocket.Payload;
import io.rsocket.frame.FrameType;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.core.publisher.Sinks;
import reactor.util.context.Context;

/**
 * Default implementation of {@link Channel} stored in {@link ChannelPool}
 */
final class PooledChannel extends ResolvingOperator<Channel> implements CoreSubscriber<Channel>, Channel {

  final ChannelPool parent;

  final Mono<Channel> rSocketSource;

  final LoadBalanceTarget loadbalanceTarget;

  final Sinks.Empty<Void> onCloseSink;

  volatile Subscription s;

  static final AtomicReferenceFieldUpdater<PooledChannel, Subscription> S =
          AtomicReferenceFieldUpdater.newUpdater(PooledChannel.class, Subscription.class, "s");

  PooledChannel(ChannelPool parent, Mono<Channel> rSocketSource, LoadBalanceTarget loadbalanceTarget) {
    this.parent = parent;
    this.rSocketSource = rSocketSource;
    this.loadbalanceTarget = loadbalanceTarget;
    this.onCloseSink = Sinks.unsafe().empty();
  }

  @Override
  public void onSubscribe(Subscription s) {
    if (Operators.setOnce(S, this, s)) {
      s.request(Long.MAX_VALUE);
    }
  }

  @Override
  public void onComplete() {
    final Subscription s = this.s;
    final Channel value = this.value;

    if (s == Operators.cancelledSubscription() || !S.compareAndSet(this, s, null)) {
      this.doFinally();
      return;
    }

    if (value == null) {
      this.terminate(new IllegalStateException("Source completed empty"));
    }
    else {
      this.complete(value);
    }
  }

  @Override
  public void onError(Throwable t) {
    final Subscription s = this.s;

    if (s == Operators.cancelledSubscription()
            || S.getAndSet(this, Operators.cancelledSubscription())
            == Operators.cancelledSubscription()) {
      this.doFinally();
      Operators.onErrorDropped(t, Context.empty());
      return;
    }

    this.doFinally();
    // terminate upstream (retryBackoff has exhausted) and remove from the parent target list
    this.doCleanup(t);
  }

  @Override
  public void onNext(Channel value) {
    if (this.s == Operators.cancelledSubscription()) {
      this.doOnValueExpired(value);
      return;
    }

    this.value = value;
    // volatile write and check on racing
    this.doFinally();
  }

  @Override
  protected void doSubscribe() {
    this.rSocketSource.subscribe(this);
  }

  @Override
  protected void doOnValueResolved(Channel value) {
    value.onClose().subscribe(null, this::doCleanup, () -> doCleanup(ON_DISPOSE));
  }

  void doCleanup(Throwable t) {
    if (isDisposed()) {
      return;
    }

    this.terminate(t);

    final ChannelPool parent = this.parent;
    for (; ; ) {
      final PooledChannel[] sockets = parent.activeSockets;
      final int activeSocketsCount = sockets.length;

      int index = -1;
      for (int i = 0; i < activeSocketsCount; i++) {
        if (sockets[i] == this) {
          index = i;
          break;
        }
      }

      if (index == -1) {
        break;
      }

      final PooledChannel[] newSockets;
      if (activeSocketsCount == 1) {
        newSockets = ChannelPool.EMPTY;
      }
      else {
        final int lastIndex = activeSocketsCount - 1;

        newSockets = new PooledChannel[lastIndex];
        if (index != 0) {
          System.arraycopy(sockets, 0, newSockets, 0, index);
        }

        if (index != lastIndex) {
          System.arraycopy(sockets, index + 1, newSockets, index, lastIndex - index);
        }
      }

      if (ChannelPool.ACTIVE_SOCKETS.compareAndSet(parent, sockets, newSockets)) {
        break;
      }
    }

    if (t == ON_DISPOSE) {
      this.onCloseSink.tryEmitEmpty();
    }
    else {
      this.onCloseSink.tryEmitError(t);
    }
  }

  @Override
  protected void doOnValueExpired(Channel value) {
    value.dispose();
  }

  @Override
  protected void doOnDispose() {
    Operators.terminate(S, this);

    final Channel value = this.value;
    if (value != null) {
      value.onClose().subscribe(null, onCloseSink::tryEmitError, onCloseSink::tryEmitEmpty);
    }
    else {
      onCloseSink.tryEmitEmpty();
    }
  }

  @Override
  public Mono<Void> fireAndForget(Payload payload) {
    return new MonoInner<>(this, payload, FrameType.REQUEST_FNF);
  }

  @Override
  public Mono<Payload> requestResponse(Payload payload) {
    return new MonoInner<>(this, payload, FrameType.REQUEST_RESPONSE);
  }

  @Override
  public Flux<Payload> requestStream(Payload payload) {
    return new FluxInner<>(this, payload, FrameType.REQUEST_STREAM);
  }

  @Override
  public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
    return new FluxInner<>(this, payloads, FrameType.REQUEST_CHANNEL);
  }

  @Override
  public Mono<Void> metadataPush(Payload payload) {
    return new MonoInner<>(this, payload, FrameType.METADATA_PUSH);
  }

  LoadBalanceTarget target() {
    return this.loadbalanceTarget;
  }

  @Override
  public Mono<Void> onClose() {
    return this.onCloseSink.asMono();
  }

  @Override
  public double availability() {
    final Channel socket = valueIfResolved();
    return socket != null ? socket.availability() : 0.0d;
  }

  static final class MonoInner<RESULT> extends MonoDeferredResolution<RESULT, Channel> {

    MonoInner(PooledChannel parent, Payload payload, FrameType requestType) {
      super(parent, payload, requestType);
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void accept(Channel channel, Throwable t) {
      if (isTerminated()) {
        return;
      }

      if (t != null) {
        ReferenceCountUtil.safeRelease(this.payload);
        onError(t);
        return;
      }

      if (channel != null) {
        Mono<?> source;
        switch (this.requestType) {
          case REQUEST_FNF:
            source = channel.fireAndForget(this.payload);
            break;
          case REQUEST_RESPONSE:
            source = channel.requestResponse(this.payload);
            break;
          case METADATA_PUSH:
            source = channel.metadataPush(this.payload);
            break;
          default:
            Operators.error(this.actual, new IllegalStateException("Should never happen"));
            return;
        }

        source.subscribe((CoreSubscriber) this);
      }
      else {
        parent.observe(this);
      }
    }
  }

  static final class FluxInner<INPUT> extends FluxDeferredResolution<INPUT, Channel> {

    FluxInner(PooledChannel parent, INPUT fluxOrPayload, FrameType requestType) {
      super(parent, fluxOrPayload, requestType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void accept(Channel channel, Throwable t) {
      if (isTerminated()) {
        return;
      }

      if (t != null) {
        if (this.requestType == FrameType.REQUEST_STREAM) {
          ReferenceCountUtil.safeRelease(this.fluxOrPayload);
        }
        onError(t);
        return;
      }

      if (channel != null) {
        Flux<? extends Payload> source;
        switch (this.requestType) {
          case REQUEST_STREAM:
            source = channel.requestStream((Payload) this.fluxOrPayload);
            break;
          case REQUEST_CHANNEL:
            source = channel.requestChannel((Flux<Payload>) this.fluxOrPayload);
            break;
          default:
            Operators.error(this.actual, new IllegalStateException("Should never happen"));
            return;
        }

        source.subscribe(this);
      }
      else {
        parent.observe(this);
      }
    }
  }
}
