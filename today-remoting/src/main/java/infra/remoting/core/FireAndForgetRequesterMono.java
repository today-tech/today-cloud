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

import org.reactivestreams.Subscription;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import infra.lang.Nullable;
import infra.remoting.Payload;
import infra.remoting.frame.FrameType;
import infra.remoting.plugins.RequestInterceptor;
import io.netty.util.IllegalReferenceCountException;
import reactor.core.CoreSubscriber;
import reactor.core.Exceptions;
import reactor.core.Scannable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;

import static infra.remoting.core.PayloadValidationUtils.INVALID_PAYLOAD_ERROR_MESSAGE;
import static infra.remoting.core.PayloadValidationUtils.isValid;
import static infra.remoting.core.SendUtils.sendReleasingPayload;
import static infra.remoting.core.StateUtils.isSubscribedOrTerminated;
import static infra.remoting.core.StateUtils.isTerminated;
import static infra.remoting.core.StateUtils.lazyTerminate;
import static infra.remoting.core.StateUtils.markSubscribed;
import static infra.remoting.core.StateUtils.markTerminated;

final class FireAndForgetRequesterMono extends Mono<Void> implements Subscription, Scannable {

  volatile long state;

  static final AtomicLongFieldUpdater<FireAndForgetRequesterMono> STATE =
          AtomicLongFieldUpdater.newUpdater(FireAndForgetRequesterMono.class, "state");

  final Payload payload;

  final ChannelSupport channel;

  FireAndForgetRequesterMono(Payload payload, ChannelSupport channel) {
    this.payload = payload;
    this.channel = channel;
  }

  @Override
  public void subscribe(CoreSubscriber<? super Void> actual) {
    long previousState = markSubscribed(STATE, this);
    if (isSubscribedOrTerminated(previousState)) {
      final IllegalStateException e =
              new IllegalStateException("FireAndForgetMono allows only a single Subscriber");

      final RequestInterceptor requestInterceptor = channel.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onReject(e, FrameType.REQUEST_FNF, null);
      }

      Operators.error(actual, e);
      return;
    }

    actual.onSubscribe(this);

    final Payload p = this.payload;
    int mtu = channel.mtu;
    try {
      if (!isValid(mtu, channel.maxFrameLength, p, false)) {
        lazyTerminate(STATE, this);

        final IllegalArgumentException e = new IllegalArgumentException(
                String.format(INVALID_PAYLOAD_ERROR_MESSAGE, channel.maxFrameLength));
        final RequestInterceptor requestInterceptor = channel.requestInterceptor;
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

      final RequestInterceptor requestInterceptor = channel.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onReject(e, FrameType.REQUEST_FNF, null);
      }

      actual.onError(e);
      return;
    }

    final int streamId;
    try {
      streamId = this.channel.getNextStreamId();
    }
    catch (Throwable t) {
      lazyTerminate(STATE, this);

      final Throwable ut = Exceptions.unwrap(t);
      final RequestInterceptor requestInterceptor = channel.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onReject(ut, FrameType.REQUEST_FNF, p.metadata());
      }

      p.release();

      actual.onError(ut);
      return;
    }

    final RequestInterceptor interceptor = channel.requestInterceptor;
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

      sendReleasingPayload(streamId, FrameType.REQUEST_FNF, mtu, p,
              channel.connection, channel.allocator, true);
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
      final IllegalStateException e = new IllegalStateException("FireAndForgetMono allows only a single Subscriber");
      final RequestInterceptor requestInterceptor = channel.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onReject(e, FrameType.REQUEST_FNF, null);
      }
      throw e;
    }

    final Payload p = this.payload;
    try {
      if (!isValid(channel.mtu, channel.maxFrameLength, p, false)) {
        lazyTerminate(STATE, this);

        final IllegalArgumentException e = new IllegalArgumentException(
                String.format(INVALID_PAYLOAD_ERROR_MESSAGE, channel.maxFrameLength));

        final RequestInterceptor requestInterceptor = channel.requestInterceptor;
        if (requestInterceptor != null) {
          requestInterceptor.onReject(e, FrameType.REQUEST_FNF, p.metadata());
        }

        p.release();

        throw e;
      }
    }
    catch (IllegalReferenceCountException e) {
      lazyTerminate(STATE, this);

      final RequestInterceptor requestInterceptor = channel.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onReject(e, FrameType.REQUEST_FNF, null);
      }

      throw Exceptions.propagate(e);
    }

    final int streamId;
    try {
      streamId = this.channel.getNextStreamId();
    }
    catch (Throwable t) {
      lazyTerminate(STATE, this);

      final RequestInterceptor requestInterceptor = channel.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onReject(Exceptions.unwrap(t), FrameType.REQUEST_FNF, p.metadata());
      }

      p.release();

      throw Exceptions.propagate(t);
    }

    final RequestInterceptor interceptor = channel.requestInterceptor;
    if (interceptor != null) {
      interceptor.onStart(streamId, FrameType.REQUEST_FNF, p.metadata());
    }

    try {
      sendReleasingPayload(streamId, FrameType.REQUEST_FNF, channel.mtu, this.payload,
              channel.connection, channel.allocator, true);
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

  @Nullable
  @Override
  public Object scanUnsafe(Scannable.Attr key) {
    return null; // no particular key to be represented, still useful in hooks
  }

  @Override
  public String stepName() {
    return "source(FireAndForgetMono)";
  }

}
