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
import infra.remoting.Connection;
import infra.remoting.Payload;
import infra.remoting.frame.CancelFrameCodec;
import infra.remoting.frame.FrameType;
import infra.remoting.plugins.RequestInterceptor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.util.IllegalReferenceCountException;
import reactor.core.CoreSubscriber;
import reactor.core.Exceptions;
import reactor.core.Scannable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;

import static infra.remoting.core.PayloadValidationUtils.INVALID_PAYLOAD_ERROR_MESSAGE;
import static infra.remoting.core.PayloadValidationUtils.isValid;
import static infra.remoting.core.ReassemblyUtils.handleNextSupport;
import static infra.remoting.core.SendUtils.sendReleasingPayload;
import static infra.remoting.core.StateUtils.addRequestN;
import static infra.remoting.core.StateUtils.hasRequested;
import static infra.remoting.core.StateUtils.isFirstFrameSent;
import static infra.remoting.core.StateUtils.isReadyToSendFirstFrame;
import static infra.remoting.core.StateUtils.isSubscribedOrTerminated;
import static infra.remoting.core.StateUtils.isTerminated;
import static infra.remoting.core.StateUtils.lazyTerminate;
import static infra.remoting.core.StateUtils.markFirstFrameSent;
import static infra.remoting.core.StateUtils.markReadyToSendFirstFrame;
import static infra.remoting.core.StateUtils.markSubscribed;
import static infra.remoting.core.StateUtils.markTerminated;

final class RequestResponseRequesterMono extends Mono<Payload>
        implements RequesterFrameHandler, LeasePermitHandler, Subscription, Scannable {

  final Payload payload;

  final ChannelSupport channel;

  @Nullable
  final RequesterLeaseTracker requesterLeaseTracker;

  volatile long state;
  static final AtomicLongFieldUpdater<RequestResponseRequesterMono> STATE =
          AtomicLongFieldUpdater.newUpdater(RequestResponseRequesterMono.class, "state");

  int streamId;
  CoreSubscriber<? super Payload> actual;
  CompositeByteBuf frames;
  boolean done;

  RequestResponseRequesterMono(Payload payload, ChannelSupport channel) {
    this.payload = payload;
    this.channel = channel;
    this.requesterLeaseTracker = channel.getRequesterLeaseTracker();
  }

  @Override
  public void subscribe(CoreSubscriber<? super Payload> actual) {
    ChannelSupport channel = this.channel;
    long previousState = markSubscribed(STATE, this);
    if (isSubscribedOrTerminated(previousState)) {
      final IllegalStateException e =
              new IllegalStateException("RequestResponseMono allows only a single " + "Subscriber");
      final RequestInterceptor requestInterceptor = channel.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onReject(e, FrameType.REQUEST_RESPONSE, null);
      }

      Operators.error(actual, e);
      return;
    }

    final Payload p = this.payload;
    try {
      if (!isValid(channel.mtu, channel.maxFrameLength, p, false)) {
        lazyTerminate(STATE, this);

        final IllegalArgumentException e = new IllegalArgumentException(String.format(INVALID_PAYLOAD_ERROR_MESSAGE, channel.maxFrameLength));
        final RequestInterceptor requestInterceptor = channel.requestInterceptor;
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

      final RequestInterceptor requestInterceptor = channel.requestInterceptor;
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

    final ChannelSupport sm = this.channel;
    final Connection connection = sm.connection;
    final ByteBufAllocator allocator = sm.allocator;

    final int streamId;
    try {
      streamId = sm.addAndGetNextStreamId(this);
      this.streamId = streamId;
    }
    catch (Throwable t) {
      this.done = true;
      final long previousState = markTerminated(STATE, this);

      final Throwable ut = Exceptions.unwrap(t);
      final RequestInterceptor requestInterceptor = channel.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onReject(ut, FrameType.REQUEST_RESPONSE, payload.metadata());
      }

      payload.release();

      if (!isTerminated(previousState)) {
        this.actual.onError(ut);
      }
      return;
    }

    final RequestInterceptor requestInterceptor = channel.requestInterceptor;
    if (requestInterceptor != null) {
      requestInterceptor.onStart(streamId, FrameType.REQUEST_RESPONSE, payload.metadata());
    }

    try {
      sendReleasingPayload(
              streamId, FrameType.REQUEST_RESPONSE, sm.mtu, payload, connection, allocator, true);
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
      ChannelSupport channel = this.channel;
      channel.remove(streamId, this);

      ReassemblyUtils.synchronizedRelease(this, previousState);

      channel.connection.sendFrame(streamId, CancelFrameCodec.encode(channel.allocator, streamId));

      final RequestInterceptor requestInterceptor = channel.requestInterceptor;
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
    this.channel.remove(streamId, this);

    final RequestInterceptor requestInterceptor = channel.requestInterceptor;
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
    this.channel.remove(streamId, this);

    final RequestInterceptor requestInterceptor = channel.requestInterceptor;
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
    final RequestInterceptor requestInterceptor = channel.requestInterceptor;
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
    this.channel.remove(streamId, this);

    final RequestInterceptor requestInterceptor = channel.requestInterceptor;
    if (requestInterceptor != null) {
      requestInterceptor.onTerminate(streamId, FrameType.REQUEST_RESPONSE, cause);
    }

    this.actual.onError(cause);
  }

  @Override
  public void handleNext(ByteBuf frame, boolean hasFollows, boolean isLastPayload) {
    handleNextSupport(STATE,
            this,
            this,
            this.actual,
            channel.payloadDecoder,
            channel.allocator,
            channel.maxInboundPayloadSize,
            frame,
            hasFollows,
            isLastPayload);
  }

  @Override
  public CompositeByteBuf getFrames() {
    return this.frames;
  }

  @Override
  public void setFrames(@Nullable CompositeByteBuf byteBuf) {
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
