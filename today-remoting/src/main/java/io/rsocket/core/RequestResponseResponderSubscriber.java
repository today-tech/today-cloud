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

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.util.IllegalReferenceCountException;
import io.netty.util.ReferenceCountUtil;
import io.rsocket.Channel;
import io.rsocket.DuplexConnection;
import io.rsocket.Payload;
import io.rsocket.exceptions.CanceledException;
import io.rsocket.frame.ErrorFrameCodec;
import io.rsocket.frame.FrameType;
import io.rsocket.frame.PayloadFrameCodec;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.plugins.RequestInterceptor;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

import static io.rsocket.core.PayloadValidationUtils.INVALID_PAYLOAD_ERROR_MESSAGE;
import static io.rsocket.core.PayloadValidationUtils.isValid;
import static io.rsocket.core.SendUtils.sendReleasingPayload;

final class RequestResponseResponderSubscriber
        implements ResponderFrameHandler, CoreSubscriber<Payload> {

  static final Logger logger = LoggerFactory.getLogger(RequestResponseResponderSubscriber.class);

  final int streamId;
  final ByteBufAllocator allocator;
  final PayloadDecoder payloadDecoder;
  final int mtu;
  final int maxFrameLength;
  final int maxInboundPayloadSize;
  final RequesterResponderSupport requesterResponderSupport;
  final DuplexConnection connection;
  final Channel handler;

  @Nullable
  final RequestInterceptor requestInterceptor;

  boolean done;
  CompositeByteBuf frames;

  volatile Subscription s;
  static final AtomicReferenceFieldUpdater<RequestResponseResponderSubscriber, Subscription> S =
          AtomicReferenceFieldUpdater.newUpdater(
                  RequestResponseResponderSubscriber.class, Subscription.class, "s");

  public RequestResponseResponderSubscriber(
          int streamId,
          ByteBuf firstFrame,
          RequesterResponderSupport requesterResponderSupport,
          Channel handler) {
    this.streamId = streamId;
    this.allocator = requesterResponderSupport.getAllocator();
    this.mtu = requesterResponderSupport.getMtu();
    this.maxFrameLength = requesterResponderSupport.getMaxFrameLength();
    this.maxInboundPayloadSize = requesterResponderSupport.getMaxInboundPayloadSize();
    this.requesterResponderSupport = requesterResponderSupport;
    this.connection = requesterResponderSupport.getDuplexConnection();
    this.payloadDecoder = requesterResponderSupport.getPayloadDecoder();
    this.requestInterceptor = requesterResponderSupport.getRequestInterceptor();
    this.handler = handler;

    this.frames =
            ReassemblyUtils.addFollowingFrame(
                    allocator.compositeBuffer(), firstFrame, true, maxInboundPayloadSize);
  }

  public RequestResponseResponderSubscriber(
          int streamId, RequesterResponderSupport requesterResponderSupport) {
    this.streamId = streamId;
    this.allocator = requesterResponderSupport.getAllocator();
    this.mtu = requesterResponderSupport.getMtu();
    this.maxFrameLength = requesterResponderSupport.getMaxFrameLength();
    this.maxInboundPayloadSize = requesterResponderSupport.getMaxInboundPayloadSize();
    this.requesterResponderSupport = requesterResponderSupport;
    this.connection = requesterResponderSupport.getDuplexConnection();
    this.requestInterceptor = requesterResponderSupport.getRequestInterceptor();

    this.payloadDecoder = null;
    this.handler = null;
    this.frames = null;
  }

  @Override
  public void onSubscribe(Subscription subscription) {
    if (Operators.validate(this.s, subscription)) {
      S.lazySet(this, subscription);
      subscription.request(Long.MAX_VALUE);
    }
  }

  @Override
  public void onNext(@Nullable Payload p) {
    if (this.done) {
      if (p != null) {
        p.release();
      }
      return;
    }

    final Subscription currentSubscription = this.s;
    if (currentSubscription == Operators.cancelledSubscription()
            || !S.compareAndSet(this, currentSubscription, Operators.cancelledSubscription())) {
      if (p != null) {
        p.release();
      }
      return;
    }

    this.done = true;

    final int streamId = this.streamId;
    final DuplexConnection connection = this.connection;
    final ByteBufAllocator allocator = this.allocator;

    this.requesterResponderSupport.remove(streamId, this);

    if (p == null) {
      final ByteBuf completeFrame = PayloadFrameCodec.encodeComplete(allocator, streamId);
      connection.sendFrame(streamId, completeFrame);

      final RequestInterceptor requestInterceptor = this.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onTerminate(streamId, FrameType.REQUEST_RESPONSE, null);
      }
      return;
    }

    final int mtu = this.mtu;
    try {
      if (!isValid(mtu, this.maxFrameLength, p, false)) {
        currentSubscription.cancel();

        p.release();

        final CanceledException e =
                new CanceledException(
                        String.format(INVALID_PAYLOAD_ERROR_MESSAGE, this.maxFrameLength));
        final ByteBuf errorFrame = ErrorFrameCodec.encode(allocator, streamId, e);
        connection.sendFrame(streamId, errorFrame);

        final RequestInterceptor requestInterceptor = this.requestInterceptor;
        if (requestInterceptor != null) {
          requestInterceptor.onTerminate(streamId, FrameType.REQUEST_RESPONSE, e);
        }
        return;
      }
    }
    catch (IllegalReferenceCountException e) {
      currentSubscription.cancel();

      final ByteBuf errorFrame =
              ErrorFrameCodec.encode(
                      allocator,
                      streamId,
                      new CanceledException("Failed to validate payload. Cause" + e.getMessage()));
      connection.sendFrame(streamId, errorFrame);

      final RequestInterceptor requestInterceptor = this.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onTerminate(streamId, FrameType.REQUEST_RESPONSE, e);
      }
      return;
    }

    try {
      sendReleasingPayload(streamId, FrameType.NEXT_COMPLETE, mtu, p, connection, allocator, false);

      final RequestInterceptor requestInterceptor = this.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onTerminate(streamId, FrameType.REQUEST_RESPONSE, null);
      }
    }
    catch (Throwable t) {
      currentSubscription.cancel();

      final RequestInterceptor requestInterceptor = this.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onTerminate(streamId, FrameType.REQUEST_RESPONSE, t);
      }
    }
  }

  @Override
  public void onError(Throwable t) {
    if (this.done) {
      logger.debug("Dropped error", t);
      return;
    }

    final Subscription currentSubscription = this.s;
    if (currentSubscription == Operators.cancelledSubscription()
            || !S.compareAndSet(this, currentSubscription, Operators.cancelledSubscription())) {
      logger.debug("Dropped error", t);
      return;
    }

    this.done = true;

    final int streamId = this.streamId;

    this.requesterResponderSupport.remove(streamId, this);

    final ByteBuf errorFrame = ErrorFrameCodec.encode(this.allocator, streamId, t);
    this.connection.sendFrame(streamId, errorFrame);

    final RequestInterceptor requestInterceptor = this.requestInterceptor;
    if (requestInterceptor != null) {
      requestInterceptor.onTerminate(streamId, FrameType.REQUEST_RESPONSE, t);
    }
  }

  @Override
  public void onComplete() {
    onNext(null);
  }

  @Override
  public void handleCancel() {
    final Subscription currentSubscription = this.s;
    if (currentSubscription == Operators.cancelledSubscription()) {
      return;
    }

    if (currentSubscription == null) {
      // if subscription is null, it means that streams has not yet reassembled all the fragments
      // and fragmentation of the first frame was cancelled before
      S.lazySet(this, Operators.cancelledSubscription());

      final int streamId = this.streamId;
      this.requesterResponderSupport.remove(streamId, this);

      final CompositeByteBuf frames = this.frames;
      if (frames != null) {
        this.frames = null;
        frames.release();
      }

      final RequestInterceptor requestInterceptor = this.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onCancel(streamId, FrameType.REQUEST_RESPONSE);
      }
      return;
    }

    if (!S.compareAndSet(this, currentSubscription, Operators.cancelledSubscription())) {
      return;
    }

    final int streamId = this.streamId;
    this.requesterResponderSupport.remove(streamId, this);

    currentSubscription.cancel();

    final RequestInterceptor requestInterceptor = this.requestInterceptor;
    if (requestInterceptor != null) {
      requestInterceptor.onCancel(streamId, FrameType.REQUEST_RESPONSE);
    }
  }

  @Override
  public void handleNext(ByteBuf frame, boolean hasFollows, boolean isLastPayload) {
    final CompositeByteBuf frames = this.frames;
    if (frames == null) {
      return;
    }

    try {
      ReassemblyUtils.addFollowingFrame(frames, frame, hasFollows, this.maxInboundPayloadSize);
    }
    catch (IllegalStateException t) {
      S.lazySet(this, Operators.cancelledSubscription());

      this.requesterResponderSupport.remove(this.streamId, this);

      this.frames = null;
      frames.release();

      logger.debug("Reassembly has failed", t);

      // sends error frame from the responder side to tell that something went wrong
      final int streamId = this.streamId;
      final ByteBuf errorFrame =
              ErrorFrameCodec.encode(
                      this.allocator,
                      streamId,
                      new CanceledException("Failed to reassemble payload. Cause: " + t.getMessage()));
      this.connection.sendFrame(streamId, errorFrame);

      final RequestInterceptor requestInterceptor = this.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onTerminate(streamId, FrameType.REQUEST_RESPONSE, t);
      }
      return;
    }

    if (!hasFollows) {
      this.frames = null;
      Payload payload;
      try {
        payload = this.payloadDecoder.apply(frames);
        frames.release();
      }
      catch (Throwable t) {
        S.lazySet(this, Operators.cancelledSubscription());

        final int streamId = this.streamId;
        this.requesterResponderSupport.remove(streamId, this);

        ReferenceCountUtil.safeRelease(frames);

        logger.debug("Reassembly has failed", t);

        // sends error frame from the responder side to tell that something went wrong
        final ByteBuf errorFrame =
                ErrorFrameCodec.encode(
                        this.allocator,
                        streamId,
                        new CanceledException("Failed to reassemble payload. Cause: " + t.getMessage()));
        this.connection.sendFrame(streamId, errorFrame);

        final RequestInterceptor requestInterceptor = this.requestInterceptor;
        if (requestInterceptor != null) {
          requestInterceptor.onTerminate(streamId, FrameType.REQUEST_RESPONSE, t);
        }
        return;
      }

      final Mono<Payload> source = this.handler.requestResponse(payload);
      source.subscribe(this);
    }
  }

  @Override
  public Context currentContext() {
    return SendUtils.DISCARD_CONTEXT;
  }
}
