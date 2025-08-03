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

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import infra.lang.NonNull;
import infra.lang.Nullable;
import infra.remoting.DuplexConnection;
import infra.remoting.Payload;
import infra.remoting.frame.FrameType;
import infra.remoting.plugins.RequestInterceptor;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.IllegalReferenceCountException;
import reactor.core.CoreSubscriber;
import reactor.core.Exceptions;
import reactor.core.Scannable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;

import static infra.remoting.core.PayloadValidationUtils.INVALID_PAYLOAD_ERROR_MESSAGE;
import static infra.remoting.core.PayloadValidationUtils.isValid;
import static infra.remoting.core.SendUtils.sendReleasingPayload;
import static infra.remoting.core.StateUtils.isReadyToSendFirstFrame;
import static infra.remoting.core.StateUtils.isSubscribedOrTerminated;
import static infra.remoting.core.StateUtils.isTerminated;
import static infra.remoting.core.StateUtils.lazyTerminate;
import static infra.remoting.core.StateUtils.markReadyToSendFirstFrame;
import static infra.remoting.core.StateUtils.markSubscribed;
import static infra.remoting.core.StateUtils.markTerminated;

final class SlowFireAndForgetRequesterMono extends Mono<Void>
        implements LeasePermitHandler, Subscription, Scannable {

  volatile long state;

  static final AtomicLongFieldUpdater<SlowFireAndForgetRequesterMono> STATE =
          AtomicLongFieldUpdater.newUpdater(SlowFireAndForgetRequesterMono.class, "state");

  final Payload payload;

  final ByteBufAllocator allocator;
  final int mtu;
  final int maxFrameLength;
  final RequesterResponderSupport requesterResponderSupport;
  final DuplexConnection connection;

  @Nullable
  final RequesterLeaseTracker requesterLeaseTracker;
  @Nullable
  final RequestInterceptor requestInterceptor;

  CoreSubscriber<? super Void> actual;

  SlowFireAndForgetRequesterMono(
          Payload payload, RequesterResponderSupport requesterResponderSupport) {
    this.allocator = requesterResponderSupport.getAllocator();
    this.payload = payload;
    this.mtu = requesterResponderSupport.getMtu();
    this.maxFrameLength = requesterResponderSupport.getMaxFrameLength();
    this.requesterResponderSupport = requesterResponderSupport;
    this.connection = requesterResponderSupport.getDuplexConnection();
    this.requestInterceptor = requesterResponderSupport.getRequestInterceptor();
    this.requesterLeaseTracker = requesterResponderSupport.getRequesterLeaseTracker();
  }

  @Override
  public void subscribe(CoreSubscriber<? super Void> actual) {
    final RequesterLeaseTracker requesterLeaseTracker = this.requesterLeaseTracker;
    final boolean leaseEnabled = requesterLeaseTracker != null;
    long previousState = markSubscribed(STATE, this, !leaseEnabled);
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

        Operators.error(actual, e);
        return;
      }
    }
    catch (IllegalReferenceCountException e) {
      lazyTerminate(STATE, this);

      final RequestInterceptor requestInterceptor = this.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onReject(e, FrameType.REQUEST_FNF, null);
      }

      Operators.error(actual, e);
      return;
    }

    this.actual = actual;
    actual.onSubscribe(this);

    if (leaseEnabled) {
      requesterLeaseTracker.issue(this);
      return;
    }

    sendFirstFrame(p);
  }

  @Override
  public boolean handlePermit() {
    final long previousState = markReadyToSendFirstFrame(STATE, this);

    if (isTerminated(previousState)) {
      return false;
    }

    sendFirstFrame(this.payload);
    return true;
  }

  void sendFirstFrame(Payload p) {
    final CoreSubscriber<? super Void> actual = this.actual;
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
    final long previousState = markTerminated(STATE, this);

    if (isTerminated(previousState)) {
      return;
    }

    if (!isReadyToSendFirstFrame(previousState)) {
      this.payload.release();
    }
  }

  @Override
  public final void handlePermitError(Throwable cause) {
    long previousState = markTerminated(STATE, this);
    if (isTerminated(previousState)) {
      Operators.onErrorDropped(cause, this.actual.currentContext());
      return;
    }

    final Payload p = this.payload;
    final RequestInterceptor requestInterceptor = this.requestInterceptor;
    if (requestInterceptor != null) {
      requestInterceptor.onReject(cause, FrameType.REQUEST_RESPONSE, p.metadata());
    }

    p.release();

    this.actual.onError(cause);
  }

  @Override
  public Object scanUnsafe(Attr key) {
    return null; // no particular key to be represented, still useful in hooks
  }

  @Override
  @NonNull
  public String stepName() {
    return "source(FireAndForgetMono)";
  }
}
