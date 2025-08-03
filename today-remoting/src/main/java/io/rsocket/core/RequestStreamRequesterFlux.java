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
import io.rsocket.frame.RequestNFrameCodec;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.plugins.RequestInterceptor;
import reactor.core.CoreSubscriber;
import reactor.core.Exceptions;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Operators;

import static io.rsocket.core.PayloadValidationUtils.INVALID_PAYLOAD_ERROR_MESSAGE;
import static io.rsocket.core.PayloadValidationUtils.isValid;
import static io.rsocket.core.ReassemblyUtils.handleNextSupport;
import static io.rsocket.core.SendUtils.sendReleasingPayload;
import static io.rsocket.core.StateUtils.addRequestN;
import static io.rsocket.core.StateUtils.extractRequestN;
import static io.rsocket.core.StateUtils.hasRequested;
import static io.rsocket.core.StateUtils.isFirstFrameSent;
import static io.rsocket.core.StateUtils.isMaxAllowedRequestN;
import static io.rsocket.core.StateUtils.isReadyToSendFirstFrame;
import static io.rsocket.core.StateUtils.isSubscribedOrTerminated;
import static io.rsocket.core.StateUtils.isTerminated;
import static io.rsocket.core.StateUtils.lazyTerminate;
import static io.rsocket.core.StateUtils.markFirstFrameSent;
import static io.rsocket.core.StateUtils.markReadyToSendFirstFrame;
import static io.rsocket.core.StateUtils.markSubscribed;
import static io.rsocket.core.StateUtils.markTerminated;

final class RequestStreamRequesterFlux extends Flux<Payload>
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
  static final AtomicLongFieldUpdater<RequestStreamRequesterFlux> STATE =
          AtomicLongFieldUpdater.newUpdater(RequestStreamRequesterFlux.class, "state");

  int streamId;
  CoreSubscriber<? super Payload> inboundSubscriber;
  CompositeByteBuf frames;
  boolean done;
  long requested;
  long produced;

  RequestStreamRequesterFlux(Payload payload, RequesterResponderSupport requesterResponderSupport) {
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
              new IllegalStateException("RequestStreamFlux allows only a single Subscriber");
      final RequestInterceptor requestInterceptor = this.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onReject(e, FrameType.REQUEST_STREAM, null);
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
          requestInterceptor.onReject(e, FrameType.REQUEST_STREAM, p.metadata());
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
        requestInterceptor.onReject(e, FrameType.REQUEST_STREAM, null);
      }

      Operators.error(actual, e);
      return;
    }

    this.inboundSubscriber = actual;
    actual.onSubscribe(this);
  }

  @Override
  public final void request(long n) {
    if (!Operators.validate(n)) {
      return;
    }

    this.requested = Operators.addCap(this.requested, n);

    final RequesterLeaseTracker requesterLeaseTracker = this.requesterLeaseTracker;
    final boolean leaseEnabled = requesterLeaseTracker != null;
    final long previousState = addRequestN(STATE, this, n, !leaseEnabled);
    if (isTerminated(previousState)) {
      return;
    }

    if (hasRequested(previousState)) {
      if (isFirstFrameSent(previousState)
              && !isMaxAllowedRequestN(extractRequestN(previousState))) {
        final int streamId = this.streamId;
        final ByteBuf requestNFrame = RequestNFrameCodec.encode(this.allocator, streamId, n);
        this.connection.sendFrame(streamId, requestNFrame);
      }
      return;
    }

    if (leaseEnabled) {
      requesterLeaseTracker.issue(this);
      return;
    }

    sendFirstPayload(this.payload, n);
  }

  @Override
  public boolean handlePermit() {
    final long previousState = markReadyToSendFirstFrame(STATE, this);

    if (isTerminated(previousState)) {
      return false;
    }

    sendFirstPayload(this.payload, extractRequestN(previousState));
    return true;
  }

  void sendFirstPayload(Payload payload, long initialRequestN) {

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
        requestInterceptor.onReject(ut, FrameType.REQUEST_STREAM, payload.metadata());
      }

      payload.release();

      if (!isTerminated(previousState)) {
        this.inboundSubscriber.onError(ut);
      }
      return;
    }

    final RequestInterceptor requestInterceptor = this.requestInterceptor;
    if (requestInterceptor != null) {
      requestInterceptor.onStart(streamId, FrameType.REQUEST_STREAM, payload.metadata());
    }

    try {
      sendReleasingPayload(
              streamId,
              FrameType.REQUEST_STREAM,
              initialRequestN,
              this.mtu,
              payload,
              connection,
              allocator,
              false);
    }
    catch (Throwable t) {
      this.done = true;
      lazyTerminate(STATE, this);

      sm.remove(streamId, this);

      if (requestInterceptor != null) {
        requestInterceptor.onTerminate(streamId, FrameType.REQUEST_STREAM, t);
      }

      this.inboundSubscriber.onError(t);
      return;
    }

    long previousState = markFirstFrameSent(STATE, this);
    if (isTerminated(previousState)) {
      if (this.done) {
        return;
      }

      final ByteBuf cancelFrame = CancelFrameCodec.encode(allocator, streamId);
      connection.sendFrame(streamId, cancelFrame);

      sm.remove(streamId, this);

      if (requestInterceptor != null) {
        requestInterceptor.onCancel(streamId, FrameType.REQUEST_STREAM);
      }
      return;
    }

    if (isMaxAllowedRequestN(initialRequestN)) {
      return;
    }

    long requestN = extractRequestN(previousState);
    if (isMaxAllowedRequestN(requestN)) {
      final ByteBuf requestNFrame = RequestNFrameCodec.encode(allocator, streamId, requestN);
      connection.sendFrame(streamId, requestNFrame);
      return;
    }

    if (requestN > initialRequestN) {
      final ByteBuf requestNFrame =
              RequestNFrameCodec.encode(allocator, streamId, requestN - initialRequestN);
      connection.sendFrame(streamId, requestNFrame);
    }
  }

  @Override
  public final void cancel() {
    final long previousState = markTerminated(STATE, this);
    if (isTerminated(previousState)) {
      return;
    }

    if (isFirstFrameSent(previousState)) {
      final int streamId = this.streamId;

      ReassemblyUtils.synchronizedRelease(this, previousState);

      this.connection.sendFrame(streamId, CancelFrameCodec.encode(this.allocator, streamId));

      this.requesterResponderSupport.remove(streamId, this);

      final RequestInterceptor requestInterceptor = this.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onCancel(streamId, FrameType.REQUEST_STREAM);
      }
    }
    else if (!isReadyToSendFirstFrame(previousState)) {
      // no need to send anything, since the first request has not happened
      this.payload.release();
    }
  }

  @Override
  public final void handlePayload(Payload p) {
    if (this.done) {
      p.release();
      return;
    }

    final long produced = this.produced;
    if (this.requested == produced) {
      p.release();

      long previousState = markTerminated(STATE, this);
      if (isTerminated(previousState)) {
        return;
      }

      final int streamId = this.streamId;

      final IllegalStateException cause =
              Exceptions.failWithOverflow(
                      "The number of messages received exceeds the number requested");
      this.connection.sendFrame(streamId, CancelFrameCodec.encode(this.allocator, streamId));

      this.requesterResponderSupport.remove(streamId, this);

      final RequestInterceptor requestInterceptor = this.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onTerminate(streamId, FrameType.REQUEST_STREAM, cause);
      }

      this.inboundSubscriber.onError(cause);
      return;
    }

    this.produced = produced + 1;

    this.inboundSubscriber.onNext(p);
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
      requestInterceptor.onTerminate(streamId, FrameType.REQUEST_STREAM, null);
    }

    this.inboundSubscriber.onComplete();
  }

  @Override
  public final void handlePermitError(Throwable cause) {
    this.done = true;

    long previousState = markTerminated(STATE, this);
    if (isTerminated(previousState)) {
      Operators.onErrorDropped(cause, this.inboundSubscriber.currentContext());
      return;
    }

    final Payload p = this.payload;
    final RequestInterceptor requestInterceptor = this.requestInterceptor;
    if (requestInterceptor != null) {
      requestInterceptor.onReject(cause, FrameType.REQUEST_STREAM, p.metadata());
    }
    p.release();

    this.inboundSubscriber.onError(cause);
  }

  @Override
  public final void handleError(Throwable cause) {
    if (this.done) {
      Operators.onErrorDropped(cause, this.inboundSubscriber.currentContext());
      return;
    }

    this.done = true;

    long previousState = markTerminated(STATE, this);
    if (isTerminated(previousState)) {
      Operators.onErrorDropped(cause, this.inboundSubscriber.currentContext());
      return;
    }

    final int streamId = this.streamId;
    this.requesterResponderSupport.remove(streamId, this);

    ReassemblyUtils.synchronizedRelease(this, previousState);

    final RequestInterceptor requestInterceptor = this.requestInterceptor;
    if (requestInterceptor != null) {
      requestInterceptor.onTerminate(streamId, FrameType.REQUEST_STREAM, cause);
    }

    this.inboundSubscriber.onError(cause);
  }

  @Override
  public void handleNext(ByteBuf frame, boolean hasFollows, boolean isLastPayload) {
    handleNextSupport(
            STATE,
            this,
            this,
            this.inboundSubscriber,
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
    if (key == Attr.REQUESTED_FROM_DOWNSTREAM)
      return extractRequestN(state);

    return null;
  }

  @Override
  @NonNull
  public String stepName() {
    return "source(RequestStreamFlux)";
  }
}
