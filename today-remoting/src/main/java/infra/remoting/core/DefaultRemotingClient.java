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
package infra.remoting.core;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;
import java.util.stream.Stream;

import infra.lang.Nullable;
import infra.remoting.Channel;
import infra.remoting.Payload;
import infra.remoting.frame.FrameType;
import io.netty.util.IllegalReferenceCountException;
import io.netty.util.ReferenceCounted;
import reactor.core.CorePublisher;
import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoOperator;
import reactor.core.publisher.Operators;
import reactor.core.publisher.Sinks;
import reactor.util.context.Context;

/**
 * Default implementation of {@link RemotingClient}
 */
class DefaultRemotingClient extends ResolvingOperator<Channel>
        implements CoreSubscriber<Channel>, CorePublisher<Channel>, RemotingClient {

  static final Consumer<?> DISCARD_ELEMENTS_CONSUMER = data -> {
    if (data instanceof ReferenceCounted rc) {
      if (rc.refCnt() > 0) {
        try {
          rc.release();
        }
        catch (IllegalReferenceCountException e) {
          // ignored
        }
      }
    }
  };

  static final Object ON_DISCARD_KEY;

  static {
    Context discardAwareContext = Operators.enableOnDiscard(null, DISCARD_ELEMENTS_CONSUMER);
    ON_DISCARD_KEY = discardAwareContext.stream().findFirst().get().getKey();
  }

  final Mono<Channel> source;

  final Sinks.Empty<Void> onDisposeSink;

  volatile Subscription s;

  static final AtomicReferenceFieldUpdater<DefaultRemotingClient, Subscription> S =
          AtomicReferenceFieldUpdater.newUpdater(DefaultRemotingClient.class, Subscription.class, "s");

  DefaultRemotingClient(Mono<Channel> source) {
    this.source = unwrapReconnectMono(source);
    this.onDisposeSink = Sinks.empty();
  }

  private Mono<Channel> unwrapReconnectMono(Mono<Channel> source) {
    return source instanceof ReconnectMono ? ((ReconnectMono<Channel>) source).getSource() : source;
  }

  @Override
  public Mono<Void> onClose() {
    return onDisposeSink.asMono();
  }

  @Override
  public Mono<Channel> source() {
    return Mono.fromDirect(this);
  }

  @Override
  public Mono<Void> fireAndForget(Mono<Payload> payloadMono) {
    return new ChannelClientMonoOperator<>(this, FrameType.REQUEST_FNF, payloadMono);
  }

  @Override
  public Mono<Payload> requestResponse(Mono<Payload> payloadMono) {
    return new ChannelClientMonoOperator<>(this, FrameType.REQUEST_RESPONSE, payloadMono);
  }

  @Override
  public Flux<Payload> requestStream(Mono<Payload> payloadMono) {
    return new ChannelClientFluxOperator<>(this, FrameType.REQUEST_STREAM, payloadMono);
  }

  @Override
  public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
    return new ChannelClientFluxOperator<>(this, FrameType.REQUEST_CHANNEL, payloads);
  }

  @Override
  public Mono<Void> metadataPush(Mono<Payload> payloadMono) {
    return new ChannelClientMonoOperator<>(this, FrameType.METADATA_PUSH, payloadMono);
  }

  @Override
  @SuppressWarnings("uncheked")
  public void subscribe(CoreSubscriber<? super Channel> actual) {
    final ResolvingOperator.MonoDeferredResolutionOperator<Channel> inner =
            new ResolvingOperator.MonoDeferredResolutionOperator<>(this, actual);
    actual.onSubscribe(inner);

    observe(inner);
  }

  @Override
  public void subscribe(Subscriber<? super Channel> s) {
    subscribe(Operators.toCoreSubscriber(s));
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
      doFinally();
      return;
    }

    if (value == null) {
      terminate(new IllegalStateException("Source completed empty"));
    }
    else {
      complete(value);
    }
  }

  @Override
  public void onError(Throwable t) {
    final Subscription s = this.s;

    if (s == Operators.cancelledSubscription()
            || S.getAndSet(this, Operators.cancelledSubscription())
            == Operators.cancelledSubscription()) {
      doFinally();
      Operators.onErrorDropped(t, Context.empty());
      return;
    }

    doFinally();
    // terminate upstream which means retryBackoff has exhausted
    terminate(t);
  }

  @Override
  public void onNext(Channel value) {
    if (this.s == Operators.cancelledSubscription()) {
      doOnValueExpired(value);
      return;
    }

    this.value = value;
    // volatile write and check on racing
    doFinally();
  }

  @Override
  protected void doSubscribe() {
    source.subscribe(this);
  }

  @Override
  protected void doOnValueResolved(Channel value) {
    value.onClose().subscribe(null, t -> invalidate(), this::invalidate);
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
      value.onClose().subscribe(null, onDisposeSink::tryEmitError, onDisposeSink::tryEmitEmpty);
    }
    else {
      onDisposeSink.tryEmitEmpty();
    }
  }

  static final class FlatMapMain<R> implements CoreSubscriber<Payload>, Context, Scannable {

    private final DefaultRemotingClient parent;
    private final CoreSubscriber<? super R> actual;

    private final FlattingInner<R> second;

    private Subscription s;

    private boolean done;

    FlatMapMain(DefaultRemotingClient parent, CoreSubscriber<? super R> actual, FrameType requestType) {
      this.parent = parent;
      this.actual = actual;
      this.second = new FlattingInner<>(parent, this, actual, requestType);
    }

    @Override
    public Context currentContext() {
      return this;
    }

    @Override
    public Stream<? extends Scannable> inners() {
      return Stream.of(second);
    }

    @Override
    @Nullable
    public Object scanUnsafe(Attr key) {
      if (key == Attr.PARENT)
        return s;
      if (key == Attr.CANCELLED)
        return second.isCancelled();
      if (key == Attr.TERMINATED)
        return done;

      return null;
    }

    @Override
    public void onSubscribe(Subscription s) {
      if (Operators.validate(this.s, s)) {
        this.s = s;
        actual.onSubscribe(second);
      }
    }

    @Override
    public void onNext(Payload payload) {
      if (done) {
        if (payload.refCnt() > 0) {
          try {
            payload.release();
          }
          catch (IllegalReferenceCountException e) {
            // ignored
          }
        }
        return;
      }
      this.done = true;

      final FlattingInner<R> inner = this.second;

      if (inner.isCancelled()) {
        if (payload.refCnt() > 0) {
          try {
            payload.release();
          }
          catch (IllegalReferenceCountException e) {
            // ignored
          }
        }
        return;
      }

      inner.payload = payload;

      if (inner.isCancelled()) {
        if (FlattingInner.PAYLOAD.compareAndSet(inner, payload, null)) {
          if (payload.refCnt() > 0) {
            try {
              payload.release();
            }
            catch (IllegalReferenceCountException e) {
              // ignored
            }
          }
        }
        return;
      }

      parent.observe(inner);
    }

    @Override
    public void onError(Throwable t) {
      if (done) {
        Operators.onErrorDropped(t, actual.currentContext());
        return;
      }
      this.done = true;

      actual.onError(t);
    }

    @Override
    public void onComplete() {
      if (this.done) {
        return;
      }
      this.done = true;

      actual.onComplete();
    }

    void request(long n) {
      this.s.request(n);
    }

    void cancel() {
      this.s.cancel();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K> K get(Object key) {
      if (key == ON_DISCARD_KEY) {
        return (K) DISCARD_ELEMENTS_CONSUMER;
      }
      return actual.currentContext().get(key);
    }

    @Override
    public boolean hasKey(Object key) {
      if (key == ON_DISCARD_KEY) {
        return true;
      }
      return actual.currentContext().hasKey(key);
    }

    @Override
    public Context put(Object key, Object value) {
      return actual
              .currentContext()
              .put(ON_DISCARD_KEY, DISCARD_ELEMENTS_CONSUMER)
              .put(key, value);
    }

    @Override
    public Context delete(Object key) {
      return actual
              .currentContext()
              .put(ON_DISCARD_KEY, DISCARD_ELEMENTS_CONSUMER)
              .delete(key);
    }

    @Override
    public int size() {
      return actual.currentContext().size() + 1;
    }

    @Override
    public Stream<Map.Entry<Object, Object>> stream() {
      return Stream.concat(
              Stream.of(new AbstractMap.SimpleImmutableEntry<>(ON_DISCARD_KEY, DISCARD_ELEMENTS_CONSUMER)),
              actual.currentContext().stream());
    }
  }

  static final class FlattingInner<T> extends DeferredResolution<T, Channel> {

    private final FlatMapMain<T> main;

    private final FrameType interactionType;

    private volatile Payload payload;

    @SuppressWarnings("rawtypes")
    static final AtomicReferenceFieldUpdater<FlattingInner, Payload> PAYLOAD =
            AtomicReferenceFieldUpdater.newUpdater(FlattingInner.class, Payload.class, "payload");

    FlattingInner(DefaultRemotingClient parent, FlatMapMain<T> main,
            CoreSubscriber<? super T> actual, FrameType interactionType) {
      super(parent, actual);

      this.main = main;
      this.interactionType = interactionType;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void accept(Channel channel, Throwable t) {
      if (isCancelled()) {
        return;
      }

      Payload payload = PAYLOAD.getAndSet(this, null);

      // means cancelled
      if (payload == null) {
        return;
      }

      if (t != null) {
        if (payload.refCnt() > 0) {
          try {
            payload.release();
          }
          catch (IllegalReferenceCountException e) {
            // ignored
          }
        }
        onError(t);
        return;
      }

      CorePublisher<?> source;
      switch (interactionType) {
        case REQUEST_FNF:
          source = channel.fireAndForget(payload);
          break;
        case REQUEST_RESPONSE:
          source = channel.requestResponse(payload);
          break;
        case REQUEST_STREAM:
          source = channel.requestStream(payload);
          break;
        case METADATA_PUSH:
          source = channel.metadataPush(payload);
          break;
        default:
          onError(new IllegalStateException("Should never happen"));
          return;
      }

      source.subscribe((CoreSubscriber) this);
    }

    @Override
    public void request(long n) {
      super.request(n);
      main.request(n);
    }

    public void cancel() {
      long state = REQUESTED.getAndSet(this, STATE_CANCELLED);
      if (state == STATE_CANCELLED) {
        return;
      }

      main.cancel();

      if (state == STATE_SUBSCRIBED) {
        s.cancel();
      }
      else {
        parent.remove(this);
        Payload payload = PAYLOAD.getAndSet(this, null);
        if (payload != null) {
          payload.release();
        }
      }
    }
  }

  static final class RequestChannelInner extends DeferredResolution<Payload, Channel> {

    private final FrameType interactionType;

    private final Publisher<Payload> upstream;

    RequestChannelInner(DefaultRemotingClient parent, Publisher<Payload> upstream,
            CoreSubscriber<? super Payload> actual, FrameType interactionType) {
      super(parent, actual);

      this.upstream = upstream;
      this.interactionType = interactionType;
    }

    @Override
    public void accept(Channel channel, Throwable t) {
      if (isCancelled()) {
        return;
      }

      if (t != null) {
        onError(t);
        return;
      }

      Flux<Payload> source;
      if (interactionType == FrameType.REQUEST_CHANNEL) {
        source = channel.requestChannel(upstream);
      }
      else {
        onError(new IllegalStateException("Should never happen"));
        return;
      }

      source.subscribe(this);
    }
  }

  static class ChannelClientMonoOperator<T> extends MonoOperator<Payload, T> {

    final DefaultRemotingClient parent;

    final FrameType requestType;

    public ChannelClientMonoOperator(DefaultRemotingClient parent, FrameType requestType, Mono<Payload> source) {
      super(source);
      this.parent = parent;
      this.requestType = requestType;
    }

    @Override
    public void subscribe(CoreSubscriber<? super T> actual) {
      source.subscribe(new FlatMapMain<T>(parent, actual, requestType));
    }
  }

  static class ChannelClientFluxOperator<ST extends Publisher<Payload>> extends Flux<Payload> {

    final DefaultRemotingClient parent;

    final FrameType requestType;

    final ST source;

    public ChannelClientFluxOperator(DefaultRemotingClient parent, FrameType requestType, ST source) {
      this.parent = parent;
      this.requestType = requestType;
      this.source = source;
    }

    @Override
    public void subscribe(CoreSubscriber<? super Payload> actual) {
      if (requestType == FrameType.REQUEST_CHANNEL) {
        RequestChannelInner inner = new RequestChannelInner(parent, source, actual, requestType);
        actual.onSubscribe(inner);
        parent.observe(inner);
      }
      else {
        source.subscribe(new FlatMapMain<>(parent, actual, requestType));
      }
    }
  }
}
