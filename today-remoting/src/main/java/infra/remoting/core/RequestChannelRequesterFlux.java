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
import org.reactivestreams.Subscription;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import infra.lang.NonNull;
import infra.lang.Nullable;
import infra.remoting.Connection;
import infra.remoting.Payload;
import infra.remoting.frame.CancelFrameCodec;
import infra.remoting.frame.ErrorFrameCodec;
import infra.remoting.frame.FrameType;
import infra.remoting.frame.PayloadFrameCodec;
import infra.remoting.frame.RequestNFrameCodec;
import infra.remoting.plugins.RequestInterceptor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.util.IllegalReferenceCountException;
import reactor.core.CoreSubscriber;
import reactor.core.Exceptions;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import static infra.remoting.core.PayloadValidationUtils.INVALID_PAYLOAD_ERROR_MESSAGE;
import static infra.remoting.core.PayloadValidationUtils.isValid;
import static infra.remoting.core.ReassemblyUtils.handleNextSupport;
import static infra.remoting.core.SendUtils.DISCARD_CONTEXT;
import static infra.remoting.core.SendUtils.sendReleasingPayload;
import static infra.remoting.core.StateUtils.addRequestN;
import static infra.remoting.core.StateUtils.extractRequestN;
import static infra.remoting.core.StateUtils.hasRequested;
import static infra.remoting.core.StateUtils.isFirstFrameSent;
import static infra.remoting.core.StateUtils.isFirstPayloadReceived;
import static infra.remoting.core.StateUtils.isInboundTerminated;
import static infra.remoting.core.StateUtils.isMaxAllowedRequestN;
import static infra.remoting.core.StateUtils.isOutboundTerminated;
import static infra.remoting.core.StateUtils.isReadyToSendFirstFrame;
import static infra.remoting.core.StateUtils.isSubscribedOrTerminated;
import static infra.remoting.core.StateUtils.isTerminated;
import static infra.remoting.core.StateUtils.markFirstFrameSent;
import static infra.remoting.core.StateUtils.markFirstPayloadReceived;
import static infra.remoting.core.StateUtils.markInboundTerminated;
import static infra.remoting.core.StateUtils.markOutboundTerminated;
import static infra.remoting.core.StateUtils.markReadyToSendFirstFrame;
import static infra.remoting.core.StateUtils.markSubscribed;
import static infra.remoting.core.StateUtils.markTerminated;

final class RequestChannelRequesterFlux extends Flux<Payload>
        implements RequesterFrameHandler, LeasePermitHandler, CoreSubscriber<Payload>, Subscription, Scannable {

  final ChannelSupport channel;

  final Publisher<Payload> payloadsPublisher;

  @Nullable
  final RequesterLeaseTracker requesterLeaseTracker;

  volatile long state;
  static final AtomicLongFieldUpdater<RequestChannelRequesterFlux> STATE =
          AtomicLongFieldUpdater.newUpdater(RequestChannelRequesterFlux.class, "state");

  int streamId;

  boolean isFirstSignal = true;
  Payload firstPayload;

  Subscription outboundSubscription;
  boolean outboundDone;
  Throwable outboundError;

  Context cachedContext;
  CoreSubscriber<? super Payload> inboundSubscriber;
  boolean inboundDone;
  long requested;
  long produced;

  CompositeByteBuf frames;

  RequestChannelRequesterFlux(Publisher<Payload> payloadsPublisher, ChannelSupport channel) {
    this.payloadsPublisher = payloadsPublisher;
    this.channel = channel;
    this.requesterLeaseTracker = channel.getRequesterLeaseTracker();
  }

  @Override
  public void subscribe(CoreSubscriber<? super Payload> actual) {
    Objects.requireNonNull(actual, "subscribe");

    long previousState = markSubscribed(STATE, this);
    if (isSubscribedOrTerminated(previousState)) {
      final IllegalStateException e =
              new IllegalStateException("RequestChannelFlux allows only a single Subscriber");
      final RequestInterceptor requestInterceptor = channel.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onReject(e, FrameType.REQUEST_CHANNEL, null);
      }

      Operators.error(actual, e);
      return;
    }

    this.inboundSubscriber = actual;
    this.payloadsPublisher.subscribe(this);
  }

  @Override
  public void onSubscribe(Subscription outboundSubscription) {
    if (Operators.validate(this.outboundSubscription, outboundSubscription)) {
      this.outboundSubscription = outboundSubscription;
      this.inboundSubscriber.onSubscribe(this);
    }
  }

  @Override
  public final void request(long n) {
    if (!Operators.validate(n)) {
      return;
    }

    this.requested = Operators.addCap(this.requested, n);

    long previousState = addRequestN(STATE, this, n, this.requesterLeaseTracker == null);
    if (isTerminated(previousState)) {
      return;
    }

    if (hasRequested(previousState)) {
      if (isFirstFrameSent(previousState)
              && !isMaxAllowedRequestN(extractRequestN(previousState))) {
        final int streamId = this.streamId;
        final ByteBuf requestNFrame = RequestNFrameCodec.encode(channel.allocator, streamId, n);
        channel.connection.sendFrame(streamId, requestNFrame);
      }
      return;
    }

    // do first request
    this.outboundSubscription.request(1);
  }

  @Override
  public void onNext(Payload p) {
    if (this.outboundDone) {
      p.release();
      return;
    }

    if (this.isFirstSignal) {
      this.isFirstSignal = false;

      final RequesterLeaseTracker requesterLeaseTracker = this.requesterLeaseTracker;
      final boolean leaseEnabled = requesterLeaseTracker != null;

      if (leaseEnabled) {
        this.firstPayload = p;

        final long previousState = markFirstPayloadReceived(STATE, this);
        if (isTerminated(previousState)) {
          this.firstPayload = null;
          p.release();
          return;
        }

        requesterLeaseTracker.issue(this);
      }
      else {
        final long state = this.state;
        if (isTerminated(state)) {
          p.release();
          return;
        }
        // TODO: check if source is Scalar | Callable | Mono
        sendFirstPayload(p, extractRequestN(state), false);
      }
    }
    else {
      sendFollowingPayload(p);
    }
  }

  @Override
  public boolean handlePermit() {
    final long previousState = markReadyToSendFirstFrame(STATE, this);

    if (isTerminated(previousState)) {
      return false;
    }

    final Payload firstPayload = this.firstPayload;
    this.firstPayload = null;

    sendFirstPayload(
            firstPayload, extractRequestN(previousState), isOutboundTerminated(previousState));
    return true;
  }

  void sendFirstPayload(Payload firstPayload, long initialRequestN, boolean completed) {
    int mtu = channel.mtu;
    try {
      if (!isValid(mtu, channel.maxFrameLength, firstPayload, true)) {
        final long previousState = markTerminated(STATE, this);

        if (isTerminated(previousState)) {
          return;
        }

        if (!isOutboundTerminated(previousState)) {
          this.outboundSubscription.cancel();
        }

        final IllegalArgumentException e = new IllegalArgumentException(String.format(INVALID_PAYLOAD_ERROR_MESSAGE, channel.maxFrameLength));
        final RequestInterceptor requestInterceptor = channel.requestInterceptor;
        if (requestInterceptor != null) {
          requestInterceptor.onReject(e, FrameType.REQUEST_CHANNEL, firstPayload.metadata());
        }

        firstPayload.release();

        this.inboundDone = true;
        this.inboundSubscriber.onError(e);
        return;
      }
    }
    catch (IllegalReferenceCountException e) {
      final long previousState = markTerminated(STATE, this);

      if (isTerminated(previousState)) {
        Operators.onErrorDropped(e, this.inboundSubscriber.currentContext());
        return;
      }

      if (!isOutboundTerminated(previousState)) {
        this.outboundSubscription.cancel();
      }

      final RequestInterceptor requestInterceptor = channel.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onReject(e, FrameType.REQUEST_CHANNEL, null);
      }

      this.inboundDone = true;
      this.inboundSubscriber.onError(e);
      return;
    }

    final ChannelSupport channel = this.channel;
    final Connection connection = channel.connection;
    final ByteBufAllocator allocator = channel.allocator;

    final int streamId;
    try {
      streamId = channel.addAndGetNextStreamId(this);
      this.streamId = streamId;
    }
    catch (Throwable t) {
      final long previousState = markTerminated(STATE, this);

      firstPayload.release();

      if (isTerminated(previousState)) {
        Operators.onErrorDropped(t, this.inboundSubscriber.currentContext());
        return;
      }

      if (!isOutboundTerminated(previousState)) {
        this.outboundSubscription.cancel();
      }

      final Throwable ut = Exceptions.unwrap(t);
      final RequestInterceptor requestInterceptor = this.channel.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onReject(ut, FrameType.REQUEST_CHANNEL, firstPayload.metadata());
      }

      this.inboundDone = true;
      this.inboundSubscriber.onError(ut);

      return;
    }

    final RequestInterceptor requestInterceptor = this.channel.requestInterceptor;
    if (requestInterceptor != null) {
      requestInterceptor.onStart(streamId, FrameType.REQUEST_CHANNEL, firstPayload.metadata());
    }

    try {
      sendReleasingPayload(
              streamId,
              FrameType.REQUEST_CHANNEL,
              initialRequestN,
              mtu,
              firstPayload,
              connection,
              allocator,
              completed);
    }
    catch (Throwable t) {
      final long previousState = markTerminated(STATE, this);

      firstPayload.release();

      if (isTerminated(previousState)) {
        Operators.onErrorDropped(t, this.inboundSubscriber.currentContext());
        return;
      }

      channel.remove(streamId, this);

      if (!isOutboundTerminated(previousState)) {
        this.outboundSubscription.cancel();
      }

      if (requestInterceptor != null) {
        requestInterceptor.onTerminate(streamId, FrameType.REQUEST_CHANNEL, t);
      }

      this.inboundDone = true;
      this.inboundSubscriber.onError(t);
      return;
    }

    long previousState = markFirstFrameSent(STATE, this);
    if (isTerminated(previousState)) {
      // now, this can be terminated in case of the following scenarios:
      //
      // 1) SendFirst is called synchronously from onNext, thus we can have
      //    handleError called before we marked first frame sent, thus we may check if
      //    inboundDone flag is true and exit execution without any further actions:
      if (this.inboundDone) {
        return;
      }

      channel.remove(streamId, this);

      // 2) SendFirst is called asynchronously on the connection event-loop. Thus, we
      // need to check if outbound error is present. Note, we check outboundError since
      // in the last scenario, cancellation may terminate the state and async
      // onComplete may set outboundDone to true. Thus, we explicitly check for
      // outboundError
      final Throwable outboundError = this.outboundError;
      if (outboundError != null) {
        final ByteBuf errorFrame = ErrorFrameCodec.encode(allocator, streamId, outboundError);
        connection.sendFrame(streamId, errorFrame);

        if (requestInterceptor != null) {
          requestInterceptor.onTerminate(streamId, FrameType.REQUEST_CHANNEL, outboundError);
        }

        this.inboundDone = true;
        this.inboundSubscriber.onError(outboundError);
      }
      else {
        // 3) SendFirst is interleaving with cancel. Thus, we need to generate cancel
        // frame
        final ByteBuf cancelFrame = CancelFrameCodec.encode(allocator, streamId);
        connection.sendFrame(streamId, cancelFrame);

        if (requestInterceptor != null) {
          requestInterceptor.onCancel(streamId, FrameType.REQUEST_CHANNEL);
        }
      }

      return;
    }

    if (!completed && isOutboundTerminated(previousState)) {
      final ByteBuf completeFrame = PayloadFrameCodec.encodeComplete(this.channel.allocator, streamId);
      connection.sendFrame(streamId, completeFrame);
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

  final void sendFollowingPayload(Payload followingPayload) {
    int streamId = this.streamId;
    int mtu = channel.mtu;

    try {
      if (!isValid(mtu, channel.maxFrameLength, followingPayload, true)) {
        followingPayload.release();

        final IllegalArgumentException e = new IllegalArgumentException(String.format(INVALID_PAYLOAD_ERROR_MESSAGE, channel.maxFrameLength));
        if (!this.tryCancel()) {
          Operators.onErrorDropped(e, this.inboundSubscriber.currentContext());
          return;
        }

        this.propagateErrorSafely(e);
        return;
      }
    }
    catch (IllegalReferenceCountException e) {
      if (!this.tryCancel()) {
        Operators.onErrorDropped(e, this.inboundSubscriber.currentContext());
        return;
      }

      this.propagateErrorSafely(e);

      return;
    }

    try {
      sendReleasingPayload(
              streamId,

              // TODO: Should be a different flag in case of the scalar
              //  source or if we know in advance upstream is mono
              FrameType.NEXT,
              mtu,
              followingPayload,
              channel.connection,
              channel.allocator,
              true);
    }
    catch (Throwable e) {
      if (!this.tryCancel()) {
        Operators.onErrorDropped(e, this.inboundSubscriber.currentContext());
        return;
      }

      this.propagateErrorSafely(e);
    }
  }

  void propagateErrorSafely(Throwable t) {
    // FIXME: must be scheduled on the connection event-loop to achieve serial
    //  behaviour on the inbound subscriber
    if (!this.inboundDone) {
      synchronized(this) {
        if (!this.inboundDone) {
          final RequestInterceptor interceptor = channel.requestInterceptor;
          if (interceptor != null) {
            interceptor.onTerminate(this.streamId, FrameType.REQUEST_CHANNEL, t);
          }

          this.inboundDone = true;
          this.inboundSubscriber.onError(t);
        }
        else {
          Operators.onErrorDropped(t, this.inboundSubscriber.currentContext());
        }
      }
    }
    else {
      Operators.onErrorDropped(t, this.inboundSubscriber.currentContext());
    }
  }

  @Override
  public final void cancel() {
    if (!tryCancel()) {
      return;
    }

    final RequestInterceptor requestInterceptor = channel.requestInterceptor;
    if (requestInterceptor != null) {
      requestInterceptor.onCancel(this.streamId, FrameType.REQUEST_CHANNEL);
    }
  }

  boolean tryCancel() {
    long previousState = markTerminated(STATE, this);
    if (isTerminated(previousState)) {
      return false;
    }

    if (!isOutboundTerminated(previousState)) {
      this.outboundSubscription.cancel();
    }

    if (!isReadyToSendFirstFrame(previousState) && isFirstPayloadReceived(previousState)) {
      final Payload firstPayload = this.firstPayload;
      this.firstPayload = null;
      firstPayload.release();
      // no need to send anything, since we have not started a stream yet (no logical wire)
      return false;
    }

    ReassemblyUtils.synchronizedRelease(this, previousState);

    final boolean firstFrameSent = isFirstFrameSent(previousState);
    if (firstFrameSent) {
      final int streamId = this.streamId;
      final ChannelSupport channel = this.channel;
      channel.remove(streamId, this);

      final ByteBuf cancelFrame = CancelFrameCodec.encode(channel.allocator, streamId);
      channel.connection.sendFrame(streamId, cancelFrame);
    }

    return firstFrameSent;
  }

  @Override
  public void onError(Throwable t) {
    if (this.outboundDone) {
      Operators.onErrorDropped(t, this.inboundSubscriber.currentContext());
      return;
    }

    this.outboundError = t;
    this.outboundDone = true;

    long previousState = markTerminated(STATE, this);
    if (isTerminated(previousState) || isOutboundTerminated(previousState)) {
      Operators.onErrorDropped(t, this.inboundSubscriber.currentContext());
      return;
    }

    if (this.isFirstSignal) {
      this.inboundDone = true;
      this.inboundSubscriber.onError(t);
      return;
    }
    else if (!isReadyToSendFirstFrame(previousState)) {
      // first signal is received but we are still waiting for lease permit to be issued,
      // thus, just propagates error to actual subscriber

      final Payload firstPayload = this.firstPayload;
      this.firstPayload = null;

      firstPayload.release();

      this.inboundDone = true;
      this.inboundSubscriber.onError(t);

      return;
    }

    ReassemblyUtils.synchronizedRelease(this, previousState);

    if (isFirstFrameSent(previousState)) {
      final int streamId = this.streamId;
      final ChannelSupport channel = this.channel;
      channel.remove(streamId, this);
      // propagates error to remote responder
      final ByteBuf errorFrame = ErrorFrameCodec.encode(channel.allocator, streamId, t);
      channel.connection.sendFrame(streamId, errorFrame);

      if (!isInboundTerminated(previousState)) {
        // FIXME: must be scheduled on the connection event-loop to achieve serial
        //  behaviour on the inbound subscriber
        synchronized(this) {
          final RequestInterceptor interceptor = channel.requestInterceptor;
          if (interceptor != null) {
            interceptor.onTerminate(streamId, FrameType.REQUEST_CHANNEL, t);
          }

          this.inboundDone = true;
          this.inboundSubscriber.onError(t);
        }
      }
      else {
        Operators.onErrorDropped(t, this.inboundSubscriber.currentContext());
      }
    }
  }

  @Override
  public void onComplete() {
    if (this.outboundDone) {
      return;
    }

    this.outboundDone = true;

    long previousState = markOutboundTerminated(STATE, this, true);
    if (isTerminated(previousState) || isOutboundTerminated(previousState)) {
      return;
    }

    if (!isFirstFrameSent(previousState)) {
      if (!isFirstPayloadReceived(previousState)) {
        // first signal, thus, just propagates error to actual subscriber
        this.inboundSubscriber.onError(new CancellationException("Empty Source"));
      }
      return;
    }

    final int streamId = this.streamId;
    final ChannelSupport channel = this.channel;
    final ByteBuf completeFrame = PayloadFrameCodec.encodeComplete(channel.allocator, streamId);

    channel.connection.sendFrame(streamId, completeFrame);

    if (isInboundTerminated(previousState)) {
      channel.remove(streamId, this);

      final RequestInterceptor interceptor = channel.requestInterceptor;
      if (interceptor != null) {
        interceptor.onTerminate(streamId, FrameType.REQUEST_CHANNEL, null);
      }
    }
  }

  @Override
  public final void handleComplete() {
    if (this.inboundDone) {
      return;
    }

    this.inboundDone = true;

    long previousState = markInboundTerminated(STATE, this);
    if (isTerminated(previousState)) {
      return;
    }

    if (isOutboundTerminated(previousState)) {
      this.channel.remove(this.streamId, this);

      final RequestInterceptor interceptor = channel.requestInterceptor;
      if (interceptor != null) {
        interceptor.onTerminate(this.streamId, FrameType.REQUEST_CHANNEL, null);
      }
    }

    this.inboundSubscriber.onComplete();
  }

  @Override
  public final void handlePermitError(Throwable cause) {
    this.inboundDone = true;

    long previousState = markTerminated(STATE, this);
    if (isTerminated(previousState) || isInboundTerminated(previousState)) {
      Operators.onErrorDropped(cause, this.inboundSubscriber.currentContext());
      return;
    }

    if (!isOutboundTerminated(previousState)) {
      this.outboundSubscription.cancel();
    }

    final Payload p = this.firstPayload;
    final RequestInterceptor interceptor = channel.requestInterceptor;
    if (interceptor != null) {
      interceptor.onReject(cause, FrameType.REQUEST_CHANNEL, p.metadata());
    }
    p.release();

    this.inboundSubscriber.onError(cause);
  }

  @Override
  public final void handleError(Throwable cause) {
    if (this.inboundDone) {
      Operators.onErrorDropped(cause, this.inboundSubscriber.currentContext());
      return;
    }

    this.inboundDone = true;

    long previousState = markTerminated(STATE, this);
    if (isTerminated(previousState) || isInboundTerminated(previousState)) {
      Operators.onErrorDropped(cause, this.inboundSubscriber.currentContext());
      return;
    }

    if (!isOutboundTerminated(previousState)) {
      this.outboundSubscription.cancel();
    }

    ReassemblyUtils.release(this, previousState);

    final int streamId = this.streamId;
    this.channel.remove(streamId, this);

    final RequestInterceptor interceptor = channel.requestInterceptor;
    if (interceptor != null) {
      interceptor.onTerminate(streamId, FrameType.REQUEST_CHANNEL, cause);
    }

    this.inboundSubscriber.onError(cause);
  }

  @Override
  public final void handlePayload(Payload value) {
    synchronized(this) {
      if (this.inboundDone) {
        value.release();
        return;
      }

      final long produced = this.produced;
      if (this.requested == produced) {
        value.release();
        if (!tryCancel()) {
          return;
        }

        final Throwable cause =
                Exceptions.failWithOverflow(
                        "The number of messages received exceeds the number requested");
        final RequestInterceptor requestInterceptor = channel.requestInterceptor;
        if (requestInterceptor != null) {
          requestInterceptor.onTerminate(streamId, FrameType.REQUEST_CHANNEL, cause);
        }

        this.inboundSubscriber.onError(cause);
        return;
      }

      this.produced = produced + 1;

      this.inboundSubscriber.onNext(value);
    }
  }

  @Override
  public void handleRequestN(long n) {
    this.outboundSubscription.request(n);
  }

  @Override
  public void handleCancel() {
    if (this.outboundDone) {
      return;
    }

    long previousState = markOutboundTerminated(STATE, this, false);
    if (isTerminated(previousState) || isOutboundTerminated(previousState)) {
      return;
    }

    final boolean inboundTerminated = isInboundTerminated(previousState);
    if (inboundTerminated) {
      this.channel.remove(this.streamId, this);
    }

    this.outboundSubscription.cancel();

    if (inboundTerminated) {
      final RequestInterceptor interceptor = channel.requestInterceptor;
      if (interceptor != null) {
        interceptor.onTerminate(this.streamId, FrameType.REQUEST_CHANNEL, null);
      }
    }
  }

  @Override
  public void handleNext(ByteBuf frame, boolean hasFollows, boolean isLastPayload) {
    handleNextSupport(
            STATE,
            this,
            this,
            this.inboundSubscriber,
            channel.payloadDecoder,
            channel.allocator,
            channel.maxInboundPayloadSize,
            frame,
            hasFollows,
            isLastPayload);
  }

  @Override
  @NonNull
  public Context currentContext() {
    long state = this.state;

    if (isSubscribedOrTerminated(state)) {
      Context cachedContext = this.cachedContext;
      if (cachedContext == null) {
        cachedContext =
                this.inboundSubscriber.currentContext().putAll((ContextView) DISCARD_CONTEXT);
        this.cachedContext = cachedContext;
      }
      return cachedContext;
    }

    return Context.empty();
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
      return state;

    return null;
  }

  @Override
  @NonNull
  public String stepName() {
    return "source(RequestChannelFlux)";
  }
}
