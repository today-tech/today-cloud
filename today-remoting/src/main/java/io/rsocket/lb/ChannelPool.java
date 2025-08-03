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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.stream.Collectors;

import infra.lang.Nullable;
import io.netty.util.ReferenceCountUtil;
import io.rsocket.Channel;
import io.rsocket.Closeable;
import io.rsocket.Payload;
import io.rsocket.core.ChannelConnector;
import io.rsocket.frame.FrameType;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.core.publisher.Sinks;

class ChannelPool extends ResolvingOperator<Object> implements CoreSubscriber<List<LoadBalanceTarget>>, Closeable {

  static final AtomicReferenceFieldUpdater<ChannelPool, PooledChannel[]> ACTIVE_SOCKETS =
          AtomicReferenceFieldUpdater.newUpdater(ChannelPool.class, PooledChannel[].class, "activeSockets");

  static final PooledChannel[] EMPTY = new PooledChannel[0];
  static final PooledChannel[] TERMINATED = new PooledChannel[0];

  static final AtomicReferenceFieldUpdater<ChannelPool, Subscription> S =
          AtomicReferenceFieldUpdater.newUpdater(ChannelPool.class, Subscription.class, "s");

  final DeferredResolutionChannel deferredResolutionRSocket = new DeferredResolutionChannel(this);

  final ChannelConnector connector;

  final LoadBalanceStrategy loadbalanceStrategy;

  final Sinks.Empty<Void> onAllClosedSink = Sinks.unsafe().empty();

  volatile PooledChannel[] activeSockets;

  volatile Subscription s;

  public ChannelPool(ChannelConnector connector, Publisher<List<LoadBalanceTarget>> targetPublisher, LoadBalanceStrategy loadbalanceStrategy) {
    this.connector = connector;
    this.loadbalanceStrategy = loadbalanceStrategy;
    ACTIVE_SOCKETS.lazySet(this, EMPTY);
    targetPublisher.subscribe(this);
  }

  @Override
  public Mono<Void> onClose() {
    return onAllClosedSink.asMono();
  }

  @Override
  protected void doOnDispose() {
    Operators.terminate(S, this);

    Channel[] activeSockets = ACTIVE_SOCKETS.getAndSet(this, TERMINATED);
    for (Channel channel : activeSockets) {
      channel.dispose();
    }

    if (activeSockets.length > 0) {
      Mono.whenDelayError(Arrays.stream(activeSockets).map(Channel::onClose).collect(Collectors.toList()))
              .subscribe(null, onAllClosedSink::tryEmitError, onAllClosedSink::tryEmitEmpty);
    }
    else {
      onAllClosedSink.tryEmitEmpty();
    }
  }

  @Override
  public void onSubscribe(Subscription s) {
    if (Operators.setOnce(S, this, s)) {
      s.request(Long.MAX_VALUE);
    }
  }

  @Override
  public void onNext(List<LoadBalanceTarget> targets) {
    if (isDisposed()) {
      return;
    }

    // This operation should happen less frequently than calls to select() (which are per request)
    // and therefore it is acceptable somewhat less efficient.

    PooledChannel[] previouslyActiveSockets;
    PooledChannel[] inactiveSockets;
    PooledChannel[] socketsToUse;
    for (; ; ) {
      HashMap<LoadBalanceTarget, Integer> rSocketSuppliersCopy = new HashMap<>(targets.size());

      int j = 0;
      for (LoadBalanceTarget target : targets) {
        rSocketSuppliersCopy.put(target, j++);
      }

      // Intersect current and new list of targets and find the ones to keep vs dispose
      previouslyActiveSockets = this.activeSockets;
      inactiveSockets = new PooledChannel[previouslyActiveSockets.length];
      PooledChannel[] nextActiveSockets =
              new PooledChannel[previouslyActiveSockets.length + rSocketSuppliersCopy.size()];
      int activeSocketsPosition = 0;
      int inactiveSocketsPosition = 0;
      for (PooledChannel rSocket : previouslyActiveSockets) {
        Integer index = rSocketSuppliersCopy.remove(rSocket.target());
        if (index == null) {
          // if one of the active rSockets is not included, we remove it and put in the
          // pending removal
          if (!rSocket.isDisposed()) {
            inactiveSockets[inactiveSocketsPosition++] = rSocket;
            // TODO: provide a meaningful algo for keeping removed rsocket in the list
            //  nextActiveSockets[position++] = rSocket;
          }
        }
        else {
          if (!rSocket.isDisposed()) {
            // keep old RSocket instance
            nextActiveSockets[activeSocketsPosition++] = rSocket;
          }
          else {
            // put newly create RSocket instance
            LoadBalanceTarget target = targets.get(index);
            nextActiveSockets[activeSocketsPosition++] =
                    new PooledChannel(this, this.connector.connect(target.getTransport()), target);
          }
        }
      }

      // The remainder are the brand new targets
      for (LoadBalanceTarget target : rSocketSuppliersCopy.keySet()) {
        nextActiveSockets[activeSocketsPosition++] =
                new PooledChannel(this, this.connector.connect(target.getTransport()), target);
      }

      if (activeSocketsPosition == 0) {
        socketsToUse = EMPTY;
      }
      else {
        socketsToUse = Arrays.copyOf(nextActiveSockets, activeSocketsPosition);
      }
      if (ACTIVE_SOCKETS.compareAndSet(this, previouslyActiveSockets, socketsToUse)) {
        break;
      }
    }

    for (PooledChannel inactiveSocket : inactiveSockets) {
      if (inactiveSocket == null) {
        break;
      }

      inactiveSocket.dispose();
    }

    if (isPending()) {
      // notifies that upstream is resolved
      if (socketsToUse != EMPTY) {
        //noinspection ConstantConditions
        complete(this);
      }
    }
  }

  @Override
  public void onError(Throwable t) {
    // indicates upstream termination
    S.set(this, Operators.cancelledSubscription());
    // propagates error and terminates the whole pool
    terminate(t);
  }

  @Override
  public void onComplete() {
    // indicates upstream termination
    S.set(this, Operators.cancelledSubscription());
  }

  public Channel select() {
    if (isDisposed()) {
      return this.deferredResolutionRSocket;
    }

    Channel selected = doSelect();

    if (selected == null) {
      if (this.s == Operators.cancelledSubscription()) {
        terminate(new CancellationException("Pool is exhausted"));
      }
      else {
        invalidate();

        // check since it is possible that between doSelect() and invalidate() we might
        // have received new sockets
        selected = doSelect();
        if (selected != null) {
          return selected;
        }
      }
      return this.deferredResolutionRSocket;
    }

    return selected;
  }

  @Nullable
  public Channel doSelect() {
    PooledChannel[] sockets = this.activeSockets;

    if (sockets == EMPTY || sockets == TERMINATED) {
      return null;
    }

    return this.loadbalanceStrategy.select(WrappingList.wrap(sockets));
  }

  static class DeferredResolutionChannel implements Channel {

    final ChannelPool parent;

    DeferredResolutionChannel(ChannelPool parent) {
      this.parent = parent;
    }

    @Override
    public Mono<Void> fireAndForget(Payload payload) {
      return new MonoInner<>(this.parent, payload, FrameType.REQUEST_FNF);
    }

    @Override
    public Mono<Payload> requestResponse(Payload payload) {
      return new MonoInner<>(this.parent, payload, FrameType.REQUEST_RESPONSE);
    }

    @Override
    public Flux<Payload> requestStream(Payload payload) {
      return new FluxInner<>(this.parent, payload, FrameType.REQUEST_STREAM);
    }

    @Override
    public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
      return new FluxInner<>(this.parent, payloads, FrameType.REQUEST_CHANNEL);
    }

    @Override
    public Mono<Void> metadataPush(Payload payload) {
      return new MonoInner<>(this.parent, payload, FrameType.METADATA_PUSH);
    }
  }

  static final class MonoInner<T> extends MonoDeferredResolution<T, Object> {

    MonoInner(ChannelPool parent, Payload payload, FrameType requestType) {
      super(parent, payload, requestType);
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void accept(Object aVoid, Throwable t) {
      if (isTerminated()) {
        return;
      }

      if (t != null) {
        ReferenceCountUtil.safeRelease(this.payload);
        onError(t);
        return;
      }

      ChannelPool parent = (ChannelPool) this.parent;
      for (; ; ) {
        Channel channel = parent.doSelect();
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

          return;
        }

        final int state = parent.add(this);

        if (state == ADDED_STATE) {
          return;
        }

        if (state == TERMINATED_STATE) {
          final Throwable error = parent.t;
          ReferenceCountUtil.safeRelease(this.payload);
          onError(error);
          return;
        }
      }
    }
  }

  static final class FluxInner<INPUT> extends FluxDeferredResolution<INPUT, Object> {

    FluxInner(ChannelPool parent, INPUT fluxOrPayload, FrameType requestType) {
      super(parent, fluxOrPayload, requestType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void accept(Object aVoid, Throwable t) {
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

      ChannelPool parent = (ChannelPool) this.parent;
      for (; ; ) {
        Channel channel = parent.doSelect();
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
          return;
        }

        final int state = parent.add(this);
        if (state == ADDED_STATE) {
          return;
        }

        if (state == TERMINATED_STATE) {
          final Throwable error = parent.t;
          if (this.requestType == FrameType.REQUEST_STREAM) {
            ReferenceCountUtil.safeRelease(this.fluxOrPayload);
          }
          onError(error);
          return;
        }
      }
    }
  }

  static final class WrappingList implements List<Channel> {

    static final ThreadLocal<WrappingList> INSTANCE = ThreadLocal.withInitial(WrappingList::new);

    private PooledChannel[] activeSockets;

    static List<Channel> wrap(PooledChannel[] activeSockets) {
      final WrappingList sockets = INSTANCE.get();
      sockets.activeSockets = activeSockets;
      return sockets;
    }

    @Override
    public Channel get(int index) {
      final PooledChannel socket = activeSockets[index];

      Channel realValue = socket.value;
      if (realValue != null) {
        return realValue;
      }

      realValue = socket.valueIfResolved();
      if (realValue != null) {
        return realValue;
      }

      return socket;
    }

    @Override
    public int size() {
      return activeSockets.length;
    }

    @Override
    public boolean isEmpty() {
      return activeSockets.length == 0;
    }

    @Override
    public Object[] toArray() {
      return activeSockets;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
      return (T[]) activeSockets;
    }

    @Override
    public boolean contains(Object o) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Channel> iterator() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(Channel weightedChannel) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends Channel> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(int index, Collection<? extends Channel> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Channel set(int index, Channel element) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void add(int index, Channel element) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Channel remove(int index) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int indexOf(Object o) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int lastIndexOf(Object o) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ListIterator<Channel> listIterator() {
      throw new UnsupportedOperationException();
    }

    @Override
    public ListIterator<Channel> listIterator(int index) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<Channel> subList(int fromIndex, int toIndex) {
      throw new UnsupportedOperationException();
    }
  }
}
