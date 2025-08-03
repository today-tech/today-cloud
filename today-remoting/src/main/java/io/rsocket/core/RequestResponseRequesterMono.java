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

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import infra.lang.NonNull;
import infra.lang.Nullable;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.util.IllegalReferenceCountException;
import io.rsocket.DuplexConnection;
import io.rsocket.Payload;
import io.rsocket.frame.CancelFrameCodec;
import io.rsocket.frame.FrameType;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.plugins.RequestInterceptor;
import reactor.core.CoreSubscriber;
import reactor.core.Exceptions;
import reactor.core.Scannable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;

import static io.rsocket.core.PayloadValidationUtils.INVALID_PAYLOAD_ERROR_MESSAGE;
import static io.rsocket.core.PayloadValidationUtils.isValid;
import static io.rsocket.core.ReassemblyUtils.handleNextSupport;
import static io.rsocket.core.SendUtils.sendReleasingPayload;
import static io.rsocket.core.StateUtils.addRequestN;
import static io.rsocket.core.StateUtils.hasRequested;
import static io.rsocket.core.StateUtils.isFirstFrameSent;
import static io.rsocket.core.StateUtils.isReadyToSendFirstFrame;
import static io.rsocket.core.StateUtils.isSubscribedOrTerminated;
import static io.rsocket.core.StateUtils.isTerminated;
import static io.rsocket.core.StateUtils.lazyTerminate;
import static io.rsocket.core.StateUtils.markFirstFrameSent;
import static io.rsocket.core.StateUtils.markReadyToSendFirstFrame;
import static io.rsocket.core.StateUtils.markSubscribed;
import static io.rsocket.core.StateUtils.markTerminated;

final class RequestResponseRequesterMono extends Mono<Payload>
        implements RequesterFrameHandler, LeasePermitHandler, Subscription, Scannable {

  final ByteBufAllocator allocator;
  final Payload payload;
  final int mtu;
  final int maxFrameLength;
  final int maxInboundPayloadSize;
  final RequesterResponderSupport requesterResponderSupport;
  final DuplexConnection connection;
  final PayloadDecoder payloadDecoder;

  @Nullable
  final RequesterLeaseTracker requesterLeaseTracker;
  @Nullable
  final RequestInterceptor requestInterceptor;

  volatile long state;
  static final AtomicLongFieldUpdater<RequestResponseRequesterMono> STATE =
          AtomicLongFieldUpdater.newUpdater(RequestResponseRequesterMono.class, "state");

  int streamId;
  CoreSubscriber<? super Payload> actual;
  CompositeByteBuf frames;
  boolean done;

  RequestResponseRequesterMono(
          Payload payload, RequesterResponderSupport requesterResponderSupport) {

    this.allocator = requesterResponderSupport.getAllocator();
    this.payload = payload;
    this.mtu = requesterResponderSupport.getMtu();
    this.maxFrameLength = requesterResponderSupport.getMaxFrameLength();
    this.maxInboundPayloadSize = requesterResponderSupport.getMaxInboundPayloadSize();
    this.requesterResponderSupport = requesterResponderSupport;
    this.connection = requesterResponderSupport.getDuplexConnection();
    this.payloadDecoder = requesterResponderSupport.getPayloadDecoder();
    this.requesterLeaseTracker = requesterResponderSupport.getRequesterLeaseTracker();
    this.requestInterceptor = requesterResponderSupport.getRequestInterceptor();
  }

  @Override
  public void subscribe(CoreSubscriber<? super Payload> actual) {

    long previousState = markSubscribed(STATE, this);
    if (isSubscribedOrTerminated(previousState)) {
      final IllegalStateException e =
              new IllegalStateException("RequestResponseMono allows only a single " + "Subscriber");
      final RequestInterceptor requestInterceptor = this.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onReject(e, FrameType.REQUEST_RESPONSE, null);
      }

      Operators.error(actual, e);
      return;
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
          requestInterceptor.onReject(e, FrameType.REQUEST_RESPONSE, p.metadata());
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
        requestInterceptor.onReject(e, FrameType.REQUEST_RESPONSE, null);
      }

      Operators.error(actual, e);
      return;
    }

    this.actual = actual;
    actual.onSubscribe(this);
  }

  @Override
  public final void request(long n) {
    if (!Operators.validate(n)) {
      return;
    }

    final RequesterLeaseTracker requesterLeaseTracker = this.requesterLeaseTracker;
    final boolean leaseEnabled = requesterLeaseTracker != null;
    final long previousState = addRequestN(STATE, this, n, !leaseEnabled);

    if (isTerminated(previousState) || hasRequested(previousState)) {
      return;
    }

    if (leaseEnabled) {
      requesterLeaseTracker.issue(this);
      return;
    }

    sendFirstPayload(this.payload);
  }

  @Override
  public boolean handlePermit() {
    final long previousState = markReadyToSendFirstFrame(STATE, this);

    if (isTerminated(previousState)) {
      return false;
    }

    sendFirstPayload(this.payload);
    return true;
  }

  void sendFirstPayload(Payload payload) {

    final RequesterResponderSupport sm = this.requesterResponderSupport;
    final DuplexConnection connection = this.connection;
    final ByteBufAllocator allocator = this.allocator;

    final int streamId;
    try {
      streamId = sm.addAndGetNextStreamId(this);
      this.streamId = streamId;
    }
    catch (Throwable t) {
      this.done = true;
      final long previousState = markTerminated(STATE, this);

      final Throwable ut = Exceptions.unwrap(t);
      final RequestInterceptor requestInterceptor = this.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onReject(ut, FrameType.REQUEST_RESPONSE, payload.metadata());
      }

      payload.release();

      if (!isTerminated(previousState)) {
        this.actual.onError(ut);
      }
      return;
    }

    final RequestInterceptor requestInterceptor = this.requestInterceptor;
    if (requestInterceptor != null) {
      requestInterceptor.onStart(streamId, FrameType.REQUEST_RESPONSE, payload.metadata());
    }

    try {
      sendReleasingPayload(
              streamId, FrameType.REQUEST_RESPONSE, this.mtu, payload, connection, allocator, true);
    }
    catch (Throwable e) {
      this.done = true;
      lazyTerminate(STATE, this);

      sm.remove(streamId, this);

      if (requestInterceptor != null) {
        requestInterceptor.onTerminate(streamId, FrameType.REQUEST_RESPONSE, e);
      }

      this.actual.onError(e);
      return;
    }

    long previousState = markFirstFrameSent(STATE, this);
    if (isTerminated(previousState)) {
      if (this.done) {
        return;
      }

      sm.remove(streamId, this);

      final ByteBuf cancelFrame = CancelFrameCodec.encode(allocator, streamId);
      connection.sendFrame(streamId, cancelFrame);

      if (requestInterceptor != null) {
        requestInterceptor.onCancel(streamId, FrameType.REQUEST_RESPONSE);
      }
    }
  }

  @Override
  public final void cancel() {
    long previousState = markTerminated(STATE, this);
    if (isTerminated(previousState)) {
      return;
    }

    if (isFirstFrameSent(previousState)) {
      final int streamId = this.streamId;
      this.requesterResponderSupport.remove(streamId, this);

      ReassemblyUtils.synchronizedRelease(this, previousState);

      this.connection.sendFrame(streamId, CancelFrameCodec.encode(this.allocator, streamId));

      final RequestInterceptor requestInterceptor = this.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onCancel(streamId, FrameType.REQUEST_RESPONSE);
      }
    }
    else if (!isReadyToSendFirstFrame(previousState)) {
      this.payload.release();
    }
  }

  @Override
  public final void handlePayload(Payload value) {
    if (this.done) {
      value.release();
      return;
    }

    this.done = true;

    long previousState = markTerminated(STATE, this);
    if (isTerminated(previousState)) {
      value.release();
      return;
    }

    final int streamId = this.streamId;
    this.requesterResponderSupport.remove(streamId, this);

    final RequestInterceptor requestInterceptor = this.requestInterceptor;
    if (requestInterceptor != null) {
      requestInterceptor.onTerminate(streamId, FrameType.REQUEST_RESPONSE, null);
    }

    final CoreSubscriber<? super Payload> a = this.actual;
    a.onNext(value);
    a.onComplete();
  }

  @Override
  public final void handleComplete() {
    if (this.done) {
      return;
    }

    this.done = true;

    long previousState = markTerminated(STATE, this);
    if (isTerminated(previousState)) {
      return;
    }

    final int streamId = this.streamId;
    this.requesterResponderSupport.remove(streamId, this);

    final RequestInterceptor requestInterceptor = this.requestInterceptor;
    if (requestInterceptor != null) {
      requestInterceptor.onTerminate(streamId, FrameType.REQUEST_RESPONSE, null);
    }

    this.actual.onComplete();
  }

  @Override
  public final void handlePermitError(Throwable cause) {
    this.done = true;

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
  public final void handleError(Throwable cause) {
    if (this.done) {
      Operators.onErrorDropped(cause, this.actual.currentContext());
      return;
    }

    this.done = true;

    long previousState = markTerminated(STATE, this);
    if (isTerminated(previousState)) {
      Operators.onErrorDropped(cause, this.actual.currentContext());
      return;
    }

    ReassemblyUtils.synchronizedRelease(this, previousState);

    final int streamId = this.streamId;
    this.requesterResponderSupport.remove(streamId, this);

    final RequestInterceptor requestInterceptor = this.requestInterceptor;
    if (requestInterceptor != null) {
      requestInterceptor.onTerminate(streamId, FrameType.REQUEST_RESPONSE, cause);
    }

    this.actual.onError(cause);
  }

  @Override
  public void handleNext(ByteBuf frame, boolean hasFollows, boolean isLastPayload) {
    handleNextSupport(
            STATE,
            this,
            this,
            this.actual,
            this.payloadDecoder,
            this.allocator,
            this.maxInboundPayloadSize,
            frame,
            hasFollows,
            isLastPayload);
  }

  @Override
  public CompositeByteBuf getFrames() {
    return this.frames;
  }

  @Override
  public void setFrames(CompositeByteBuf byteBuf) {
    this.frames = byteBuf;
  }

  @Override
  @Nullable
  public Object scanUnsafe(Attr key) {
    // touch guard
    long state = this.state;

    if (key == Attr.TERMINATED)
      return isTerminated(state);
    if (key == Attr.PREFETCH)
      return 0;

    return null;
  }

  @Override
  @NonNull
  public String stepName() {
    return "source(RequestResponseMono)";
  }
}
