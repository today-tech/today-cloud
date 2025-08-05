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

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.remoting.Channel;
import infra.remoting.Connection;
import infra.remoting.Payload;
import infra.remoting.error.CanceledException;
import infra.remoting.frame.ErrorFrameCodec;
import infra.remoting.frame.FrameType;
import infra.remoting.frame.PayloadFrameCodec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.util.IllegalReferenceCountException;
import io.netty.util.ReferenceCountUtil;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

import static infra.remoting.core.PayloadValidationUtils.INVALID_PAYLOAD_ERROR_MESSAGE;
import static infra.remoting.core.PayloadValidationUtils.isValid;
import static infra.remoting.core.SendUtils.sendReleasingPayload;

final class RequestStreamResponderSubscriber implements ResponderFrameHandler, CoreSubscriber<Payload> {

  static final Logger logger = LoggerFactory.getLogger(RequestStreamResponderSubscriber.class);

  final int streamId;
  final long firstRequest;

  final ChannelSupport channel;

  final Channel handler;

  volatile Subscription s;
  static final AtomicReferenceFieldUpdater<RequestStreamResponderSubscriber, Subscription> S =
          AtomicReferenceFieldUpdater.newUpdater(
                  RequestStreamResponderSubscriber.class, Subscription.class, "s");

  CompositeByteBuf frames;
  boolean done;

  public RequestStreamResponderSubscriber(int streamId, long firstRequest,
          ByteBuf firstFrame, ChannelSupport channel, Channel handler) {
    this.streamId = streamId;
    this.firstRequest = firstRequest;
    this.channel = channel;
    this.handler = handler;
    this.frames = ReassemblyUtils.addFollowingFrame(
            channel.allocator.compositeBuffer(), firstFrame, true, channel.maxInboundPayloadSize);
  }

  public RequestStreamResponderSubscriber(int streamId, long firstRequest, ChannelSupport channel) {
    this.streamId = streamId;
    this.firstRequest = firstRequest;
    this.channel = channel;

    this.handler = null;
    this.frames = null;
  }

  @Override
  public void onSubscribe(Subscription subscription) {
    if (Operators.validate(this.s, subscription)) {
      final long firstRequest = this.firstRequest;
      S.lazySet(this, subscription);
      subscription.request(firstRequest);
    }
  }

  @Override
  public void onNext(Payload p) {
    if (this.done) {
      ReferenceCountUtil.safeRelease(p);
      return;
    }

    final int streamId = this.streamId;
    final Connection sender = channel.connection;
    final ByteBufAllocator allocator = channel.allocator;

    final int mtu = channel.mtu;
    try {
      if (!isValid(mtu, channel.maxFrameLength, p, false)) {
        p.release();

        if (!this.tryTerminateOnError()) {
          return;
        }

        final CanceledException e = new CanceledException(
                String.format(INVALID_PAYLOAD_ERROR_MESSAGE, channel.maxFrameLength));
        final ByteBuf errorFrame = ErrorFrameCodec.encode(allocator, streamId, e);
        sender.sendFrame(streamId, errorFrame);

        this.channel.remove(streamId, this);

        final var interceptor = channel.requestInterceptor;
        if (interceptor != null) {
          interceptor.onTerminate(streamId, FrameType.REQUEST_STREAM, e);
        }
        return;
      }
    }
    catch (IllegalReferenceCountException e) {
      if (!this.tryTerminateOnError()) {
        return;
      }

      final ByteBuf errorFrame = ErrorFrameCodec.encode(allocator, streamId,
              new CanceledException("Failed to validate payload. Cause" + e.getMessage()));
      sender.sendFrame(streamId, errorFrame);

      this.channel.remove(streamId, this);

      final var interceptor = channel.requestInterceptor;
      if (interceptor != null) {
        interceptor.onTerminate(streamId, FrameType.REQUEST_STREAM, e);
      }
      return;
    }

    try {
      sendReleasingPayload(streamId, FrameType.NEXT, mtu, p, sender, allocator, false);
    }
    catch (Throwable t) {
      if (!this.tryTerminateOnError()) {
        return;
      }

      this.channel.remove(streamId, this);

      final var interceptor = channel.requestInterceptor;
      if (interceptor != null) {
        interceptor.onTerminate(streamId, FrameType.REQUEST_STREAM, t);
      }
    }
  }

  boolean tryTerminateOnError() {
    final Subscription currentSubscription = this.s;
    if (currentSubscription == Operators.cancelledSubscription()) {
      return false;
    }

    this.done = true;

    if (!S.compareAndSet(this, currentSubscription, Operators.cancelledSubscription())) {
      return false;
    }

    currentSubscription.cancel();

    return true;
  }

  @Override
  public void onError(Throwable t) {
    if (this.done) {
      logger.debug("Dropped error", t);
      return;
    }

    this.done = true;

    if (S.getAndSet(this, Operators.cancelledSubscription()) == Operators.cancelledSubscription()) {
      logger.debug("Dropped error", t);
      return;
    }

    final CompositeByteBuf frames = this.frames;
    if (frames != null && frames.refCnt() > 0) {
      frames.release();
    }

    final int streamId = this.streamId;

    final ByteBuf errorFrame = ErrorFrameCodec.encode(channel.allocator, streamId, t);
    channel.connection.sendFrame(streamId, errorFrame);

    this.channel.remove(streamId, this);

    final var interceptor = channel.requestInterceptor;
    if (interceptor != null) {
      interceptor.onTerminate(streamId, FrameType.REQUEST_STREAM, t);
    }
  }

  @Override
  public void onComplete() {
    if (this.done) {
      return;
    }

    this.done = true;

    if (S.getAndSet(this, Operators.cancelledSubscription()) == Operators.cancelledSubscription()) {
      return;
    }

    final int streamId = this.streamId;

    final ByteBuf completeFrame = PayloadFrameCodec.encodeComplete(channel.allocator, streamId);
    channel.connection.sendFrame(streamId, completeFrame);

    this.channel.remove(streamId, this);

    final var interceptor = channel.requestInterceptor;
    if (interceptor != null) {
      interceptor.onTerminate(streamId, FrameType.REQUEST_STREAM, null);
    }
  }

  @Override
  public void handleRequestN(long n) {
    this.s.request(n);
  }

  @Override
  public final void handleCancel() {
    final Subscription currentSubscription = this.s;
    if (currentSubscription == Operators.cancelledSubscription()) {
      return;
    }

    if (currentSubscription == null) {
      // if subscription is null, it means that streams has not yet reassembled all the fragments
      // and fragmentation of the first frame was cancelled before
      S.lazySet(this, Operators.cancelledSubscription());

      final int streamId = this.streamId;
      this.channel.remove(streamId, this);

      final CompositeByteBuf frames = this.frames;
      if (frames != null) {
        this.frames = null;
        frames.release();
      }

      final var interceptor = channel.requestInterceptor;
      if (interceptor != null) {
        interceptor.onCancel(streamId, FrameType.REQUEST_STREAM);
      }
      return;
    }

    if (!S.compareAndSet(this, currentSubscription, Operators.cancelledSubscription())) {
      return;
    }

    final int streamId = this.streamId;
    this.channel.remove(streamId, this);

    currentSubscription.cancel();

    final var interceptor = channel.requestInterceptor;
    if (interceptor != null) {
      interceptor.onCancel(streamId, FrameType.REQUEST_STREAM);
    }
  }

  @Override
  public void handleNext(ByteBuf followingFrame, boolean hasFollows, boolean isLastPayload) {
    final CompositeByteBuf frames = this.frames;
    if (frames == null) {
      return;
    }

    try {
      ReassemblyUtils.addFollowingFrame(frames, followingFrame, hasFollows, channel.maxInboundPayloadSize);
    }
    catch (IllegalStateException e) {
      // if subscription is null, it means that streams has not yet reassembled all the fragments
      // and fragmentation of the first frame was cancelled before
      S.lazySet(this, Operators.cancelledSubscription());

      final int streamId = this.streamId;

      this.frames = null;
      frames.release();

      // sends error frame from the responder side to tell that something went wrong
      final ByteBuf errorFrame = ErrorFrameCodec.encode(channel.allocator, streamId,
              new CanceledException("Failed to reassemble payload. Cause: " + e.getMessage()));
      channel.connection.sendFrame(streamId, errorFrame);

      this.channel.remove(streamId, this);

      final var interceptor = channel.requestInterceptor;
      if (interceptor != null) {
        interceptor.onTerminate(streamId, FrameType.REQUEST_STREAM, e);
      }

      logger.debug("Reassembly has failed", e);
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
        this.done = true;

        final int streamId = this.streamId;

        ReferenceCountUtil.safeRelease(frames);

        // sends error frame from the responder side to tell that something went wrong
        final ByteBuf errorFrame = ErrorFrameCodec.encode(channel.allocator, streamId,
                new CanceledException("Failed to reassemble payload. Cause: " + t.getMessage()));
        channel.connection.sendFrame(streamId, errorFrame);

        this.channel.remove(streamId, this);

        final var interceptor = channel.requestInterceptor;
        if (interceptor != null) {
          interceptor.onTerminate(streamId, FrameType.REQUEST_STREAM, t);
        }

        logger.debug("Reassembly has failed", t);
        return;
      }

      Flux<Payload> source = this.handler.requestStream(payload);
      source.subscribe(this);
    }
  }

  @Override
  public Context currentContext() {
    return SendUtils.DISCARD_CONTEXT;
  }
}
