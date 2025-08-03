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
package io.rsocket.core;

import org.reactivestreams.Subscription;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import infra.lang.NonNull;
import infra.lang.Nullable;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.IllegalReferenceCountException;
import io.rsocket.DuplexConnection;
import io.rsocket.Payload;
import io.rsocket.frame.FrameType;
import io.rsocket.plugins.RequestInterceptor;
import reactor.core.CoreSubscriber;
import reactor.core.Exceptions;
import reactor.core.Scannable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;

import static io.rsocket.core.PayloadValidationUtils.INVALID_PAYLOAD_ERROR_MESSAGE;
import static io.rsocket.core.PayloadValidationUtils.isValid;
import static io.rsocket.core.SendUtils.sendReleasingPayload;
import static io.rsocket.core.StateUtils.isSubscribedOrTerminated;
import static io.rsocket.core.StateUtils.isTerminated;
import static io.rsocket.core.StateUtils.lazyTerminate;
import static io.rsocket.core.StateUtils.markSubscribed;
import static io.rsocket.core.StateUtils.markTerminated;

final class FireAndForgetRequesterMono extends Mono<Void> implements Subscription, Scannable {

  volatile long state;

  static final AtomicLongFieldUpdater<FireAndForgetRequesterMono> STATE =
          AtomicLongFieldUpdater.newUpdater(FireAndForgetRequesterMono.class, "state");

  final Payload payload;

  final ByteBufAllocator allocator;
  final int mtu;
  final int maxFrameLength;
  final RequesterResponderSupport requesterResponderSupport;
  final DuplexConnection connection;

  @Nullable
  final RequestInterceptor requestInterceptor;

  FireAndForgetRequesterMono(Payload payload, RequesterResponderSupport requesterResponderSupport) {
    this.allocator = requesterResponderSupport.getAllocator();
    this.payload = payload;
    this.mtu = requesterResponderSupport.getMtu();
    this.maxFrameLength = requesterResponderSupport.getMaxFrameLength();
    this.requesterResponderSupport = requesterResponderSupport;
    this.connection = requesterResponderSupport.getDuplexConnection();
    this.requestInterceptor = requesterResponderSupport.getRequestInterceptor();
  }

  @Override
  public void subscribe(CoreSubscriber<? super Void> actual) {
    long previousState = markSubscribed(STATE, this);
    if (isSubscribedOrTerminated(previousState)) {
      final IllegalStateException e =
              new IllegalStateException("FireAndForgetMono allows only a single Subscriber");

      final RequestInterceptor requestInterceptor = this.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onReject(e, FrameType.REQUEST_FNF, null);
      }

      Operators.error(actual, e);
      return;
    }

    actual.onSubscribe(this);

    final Payload p = this.payload;
    int mtu = this.mtu;
    try {
      if (!isValid(mtu, this.maxFrameLength, p, false)) {
        lazyTerminate(STATE, this);

        final IllegalArgumentException e =
                new IllegalArgumentException(
                        String.format(INVALID_PAYLOAD_ERROR_MESSAGE, this.maxFrameLength));
        final RequestInterceptor requestInterceptor = this.requestInterceptor;
        if (requestInterceptor != null) {
          requestInterceptor.onReject(e, FrameType.REQUEST_FNF, p.metadata());
        }

        p.release();

        actual.onError(e);
        return;
      }
    }
    catch (IllegalReferenceCountException e) {
      lazyTerminate(STATE, this);

      final RequestInterceptor requestInterceptor = this.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onReject(e, FrameType.REQUEST_FNF, null);
      }

      actual.onError(e);
      return;
    }

    final int streamId;
    try {
      streamId = this.requesterResponderSupport.getNextStreamId();
    }
    catch (Throwable t) {
      lazyTerminate(STATE, this);

      final Throwable ut = Exceptions.unwrap(t);
      final RequestInterceptor requestInterceptor = this.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onReject(ut, FrameType.REQUEST_FNF, p.metadata());
      }

      p.release();

      actual.onError(ut);
      return;
    }

    final RequestInterceptor interceptor = this.requestInterceptor;
    if (interceptor != null) {
      interceptor.onStart(streamId, FrameType.REQUEST_FNF, p.metadata());
    }

    try {
      if (isTerminated(this.state)) {
        p.release();

        if (interceptor != null) {
          interceptor.onCancel(streamId, FrameType.REQUEST_FNF);
        }

        return;
      }

      sendReleasingPayload(
              streamId, FrameType.REQUEST_FNF, mtu, p, this.connection, this.allocator, true);
    }
    catch (Throwable e) {
      lazyTerminate(STATE, this);

      if (interceptor != null) {
        interceptor.onTerminate(streamId, FrameType.REQUEST_FNF, e);
      }

      actual.onError(e);
      return;
    }

    lazyTerminate(STATE, this);

    if (interceptor != null) {
      interceptor.onTerminate(streamId, FrameType.REQUEST_FNF, null);
    }

    actual.onComplete();
  }

  @Override
  public void request(long n) {
    // no ops
  }

  @Override
  public void cancel() {
    markTerminated(STATE, this);
  }

  @Override
  @Nullable
  public Void block(Duration m) {
    return block();
  }

  /**
   * This method is deliberately non-blocking regardless it is named as `.block`. The main intent to
   * keep this method along with the {@link #subscribe()} is to eliminate redundancy which comes
   * with a default block method implementation.
   */
  @Override
  @Nullable
  public Void block() {
    long previousState = markSubscribed(STATE, this);
    if (isSubscribedOrTerminated(previousState)) {
      final IllegalStateException e =
              new IllegalStateException("FireAndForgetMono allows only a single Subscriber");
      final RequestInterceptor requestInterceptor = this.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onReject(e, FrameType.REQUEST_FNF, null);
      }
      throw e;
    }

    final Payload p = this.payload;
    try {
      if (!isValid(this.mtu, this.maxFrameLength, p, false)) {
        lazyTerminate(STATE, this);

        final IllegalArgumentException e =
                new IllegalArgumentException(
                        String.format(INVALID_PAYLOAD_ERROR_MESSAGE, this.maxFrameLength));

        final RequestInterceptor requestInterceptor = this.requestInterceptor;
        if (requestInterceptor != null) {
          requestInterceptor.onReject(e, FrameType.REQUEST_FNF, p.metadata());
        }

        p.release();

        throw e;
      }
    }
    catch (IllegalReferenceCountException e) {
      lazyTerminate(STATE, this);

      final RequestInterceptor requestInterceptor = this.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onReject(e, FrameType.REQUEST_FNF, null);
      }

      throw Exceptions.propagate(e);
    }

    final int streamId;
    try {
      streamId = this.requesterResponderSupport.getNextStreamId();
    }
    catch (Throwable t) {
      lazyTerminate(STATE, this);

      final RequestInterceptor requestInterceptor = this.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onReject(Exceptions.unwrap(t), FrameType.REQUEST_FNF, p.metadata());
      }

      p.release();

      throw Exceptions.propagate(t);
    }

    final RequestInterceptor interceptor = this.requestInterceptor;
    if (interceptor != null) {
      interceptor.onStart(streamId, FrameType.REQUEST_FNF, p.metadata());
    }

    try {
      sendReleasingPayload(
              streamId,
              FrameType.REQUEST_FNF,
              this.mtu,
              this.payload,
              this.connection,
              this.allocator,
              true);
    }
    catch (Throwable e) {
      lazyTerminate(STATE, this);

      if (interceptor != null) {
        interceptor.onTerminate(streamId, FrameType.REQUEST_FNF, e);
      }

      throw Exceptions.propagate(e);
    }

    lazyTerminate(STATE, this);

    if (interceptor != null) {
      interceptor.onTerminate(streamId, FrameType.REQUEST_FNF, null);
    }

    return null;
  }

  @Override
  public Object scanUnsafe(Scannable.Attr key) {
    return null; // no particular key to be represented, still useful in hooks
  }

  @Override
  @NonNull
  public String stepName() {
    return "source(FireAndForgetMono)";
  }
}
