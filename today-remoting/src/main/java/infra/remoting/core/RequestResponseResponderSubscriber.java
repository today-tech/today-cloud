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

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.remoting.Channel;
import infra.remoting.Connection;
import infra.remoting.Payload;
import infra.remoting.error.CanceledException;
import infra.remoting.frame.ErrorFrameCodec;
import infra.remoting.frame.FrameType;
import infra.remoting.frame.PayloadFrameCodec;
import infra.remoting.plugins.RequestInterceptor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.util.IllegalReferenceCountException;
import io.netty.util.ReferenceCountUtil;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

import static infra.remoting.core.PayloadValidationUtils.INVALID_PAYLOAD_ERROR_MESSAGE;
import static infra.remoting.core.PayloadValidationUtils.isValid;
import static infra.remoting.core.SendUtils.sendReleasingPayload;

final class RequestResponseResponderSubscriber
        implements ResponderFrameHandler, CoreSubscriber<Payload> {

  static final Logger logger = LoggerFactory.getLogger(RequestResponseResponderSubscriber.class);

  final int streamId;

  final ChannelSupport channel;

  final Channel handler;

  boolean done;
  CompositeByteBuf frames;

  volatile Subscription s;
  static final AtomicReferenceFieldUpdater<RequestResponseResponderSubscriber, Subscription> S =
          AtomicReferenceFieldUpdater.newUpdater(
                  RequestResponseResponderSubscriber.class, Subscription.class, "s");

  public RequestResponseResponderSubscriber(int streamId, ByteBuf firstFrame, ChannelSupport channel, Channel handler) {
    this.streamId = streamId;
    this.channel = channel;
    this.handler = handler;

    this.frames = ReassemblyUtils.addFollowingFrame(
            channel.allocator.compositeBuffer(), firstFrame, true, channel.maxInboundPayloadSize);
  }

  public RequestResponseResponderSubscriber(int streamId, ChannelSupport channel) {
    this.streamId = streamId;
    this.channel = channel;
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
    final ChannelSupport channel = this.channel;
    final Connection connection = channel.connection;
    final ByteBufAllocator allocator = channel.allocator;

    channel.remove(streamId, this);

    if (p == null) {
      final ByteBuf completeFrame = PayloadFrameCodec.encodeComplete(allocator, streamId);
      connection.sendFrame(streamId, completeFrame);

      final RequestInterceptor interceptor = channel.requestInterceptor;
      if (interceptor != null) {
        interceptor.onTerminate(streamId, FrameType.REQUEST_RESPONSE, null);
      }
      return;
    }

    final int mtu = channel.mtu;
    try {
      if (!isValid(mtu, channel.maxFrameLength, p, false)) {
        currentSubscription.cancel();

        p.release();

        final CanceledException e = new CanceledException(String.format(INVALID_PAYLOAD_ERROR_MESSAGE, channel.maxFrameLength));
        final ByteBuf errorFrame = ErrorFrameCodec.encode(allocator, streamId, e);
        connection.sendFrame(streamId, errorFrame);

        final var interceptor = channel.requestInterceptor;
        if (interceptor != null) {
          interceptor.onTerminate(streamId, FrameType.REQUEST_RESPONSE, e);
        }
        return;
      }
    }
    catch (IllegalReferenceCountException e) {
      currentSubscription.cancel();

      final ByteBuf errorFrame = ErrorFrameCodec.encode(allocator, streamId,
              new CanceledException("Failed to validate payload. Cause" + e.getMessage()));
      connection.sendFrame(streamId, errorFrame);

      final var interceptor = channel.requestInterceptor;
      if (interceptor != null) {
        interceptor.onTerminate(streamId, FrameType.REQUEST_RESPONSE, e);
      }
      return;
    }

    try {
      sendReleasingPayload(streamId, FrameType.NEXT_COMPLETE, mtu, p, connection, allocator, false);

      final var interceptor = channel.requestInterceptor;
      if (interceptor != null) {
        interceptor.onTerminate(streamId, FrameType.REQUEST_RESPONSE, null);
      }
    }
    catch (Throwable t) {
      currentSubscription.cancel();

      final var interceptor = channel.requestInterceptor;
      if (interceptor != null) {
        interceptor.onTerminate(streamId, FrameType.REQUEST_RESPONSE, t);
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
    final ChannelSupport channel = this.channel;

    channel.remove(streamId, this);

    final ByteBuf errorFrame = ErrorFrameCodec.encode(channel.allocator, streamId, t);
    channel.connection.sendFrame(streamId, errorFrame);

    final var interceptor = channel.requestInterceptor;
    if (interceptor != null) {
      interceptor.onTerminate(streamId, FrameType.REQUEST_RESPONSE, t);
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

    final ChannelSupport channel = this.channel;
    if (currentSubscription == null) {
      // if subscription is null, it means that streams has not yet reassembled all the fragments
      // and fragmentation of the first frame was cancelled before
      S.lazySet(this, Operators.cancelledSubscription());

      final int streamId = this.streamId;
      channel.remove(streamId, this);

      final CompositeByteBuf frames = this.frames;
      if (frames != null) {
        this.frames = null;
        frames.release();
      }

      final var interceptor = channel.requestInterceptor;
      if (interceptor != null) {
        interceptor.onCancel(streamId, FrameType.REQUEST_RESPONSE);
      }
      return;
    }

    if (!S.compareAndSet(this, currentSubscription, Operators.cancelledSubscription())) {
      return;
    }

    final int streamId = this.streamId;
    channel.remove(streamId, this);

    currentSubscription.cancel();

    final var interceptor = channel.requestInterceptor;
    if (interceptor != null) {
      interceptor.onCancel(streamId, FrameType.REQUEST_RESPONSE);
    }
  }

  @Override
  public void handleNext(ByteBuf frame, boolean hasFollows, boolean isLastPayload) {
    final CompositeByteBuf frames = this.frames;
    if (frames == null) {
      return;
    }

    final ChannelSupport channel = this.channel;
    try {
      ReassemblyUtils.addFollowingFrame(frames, frame, hasFollows, channel.maxInboundPayloadSize);
    }
    catch (IllegalStateException t) {
      S.lazySet(this, Operators.cancelledSubscription());

      channel.remove(this.streamId, this);

      this.frames = null;
      frames.release();

      logger.debug("Reassembly has failed", t);

      // sends error frame from the responder side to tell that something went wrong
      final int streamId = this.streamId;
      final ByteBuf errorFrame = ErrorFrameCodec.encode(channel.allocator, streamId,
              new CanceledException("Failed to reassemble payload. Cause: " + t.getMessage()));
      channel.connection.sendFrame(streamId, errorFrame);

      final var interceptor = channel.requestInterceptor;
      if (interceptor != null) {
        interceptor.onTerminate(streamId, FrameType.REQUEST_RESPONSE, t);
      }
      return;
    }

    if (!hasFollows) {
      this.frames = null;
      Payload payload;
      try {
        payload = channel.payloadDecoder.decode(frames);
        frames.release();
      }
      catch (Throwable t) {
        S.lazySet(this, Operators.cancelledSubscription());

        final int streamId = this.streamId;
        channel.remove(streamId, this);

        ReferenceCountUtil.safeRelease(frames);

        logger.debug("Reassembly has failed", t);

        // sends error frame from the responder side to tell that something went wrong
        final ByteBuf errorFrame = ErrorFrameCodec.encode(channel.allocator, streamId,
                new CanceledException("Failed to reassemble payload. Cause: " + t.getMessage()));
        channel.connection.sendFrame(streamId, errorFrame);

        final var interceptor = channel.requestInterceptor;
        if (interceptor != null) {
          interceptor.onTerminate(streamId, FrameType.REQUEST_RESPONSE, t);
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
