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

import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.remoting.Channel;
import infra.remoting.Payload;
import infra.remoting.frame.FrameType;
import infra.remoting.plugins.RequestInterceptor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.util.ReferenceCountUtil;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;

final class FireAndForgetResponderSubscriber implements CoreSubscriber<Void>, ResponderFrameHandler {

  static final Logger logger = LoggerFactory.getLogger(FireAndForgetResponderSubscriber.class);

  static final FireAndForgetResponderSubscriber INSTANCE = new FireAndForgetResponderSubscriber();

  final int streamId;

  final ChannelSupport channel;

  final Channel handler;

  @Nullable
  final RequestInterceptor requestInterceptor;

  CompositeByteBuf frames;

  private FireAndForgetResponderSubscriber() {
    this.streamId = 0;
    this.channel = null;
    this.handler = null;
    this.requestInterceptor = null;
    this.frames = null;
  }

  FireAndForgetResponderSubscriber(int streamId, ChannelSupport channel) {
    this.streamId = streamId;
    this.channel = null;
    this.handler = null;
    this.requestInterceptor = channel.getRequestInterceptor();
    this.frames = null;
  }

  FireAndForgetResponderSubscriber(int streamId, ByteBuf firstFrame, ChannelSupport channel, Channel handler) {
    this.streamId = streamId;
    this.channel = channel;
    this.handler = handler;
    this.requestInterceptor = channel.getRequestInterceptor();

    this.frames = ReassemblyUtils.addFollowingFrame(
            channel.allocator.compositeBuffer(), firstFrame, true, channel.maxInboundPayloadSize);
  }

  @Override
  public void onSubscribe(Subscription s) {
    s.request(Long.MAX_VALUE);
  }

  @Override
  public void onNext(Void voidVal) { }

  @Override
  public void onError(Throwable t) {
    final RequestInterceptor requestInterceptor = this.requestInterceptor;
    if (requestInterceptor != null) {
      requestInterceptor.onTerminate(this.streamId, FrameType.REQUEST_FNF, t);
    }

    logger.debug("Dropped Outbound error", t);
  }

  @Override
  public void onComplete() {
    final RequestInterceptor requestInterceptor = this.requestInterceptor;
    if (requestInterceptor != null) {
      requestInterceptor.onTerminate(this.streamId, FrameType.REQUEST_FNF, null);
    }
  }

  @Override
  public void handleNext(ByteBuf followingFrame, boolean hasFollows, boolean isLastPayload) {
    final CompositeByteBuf frames = this.frames;
    final ChannelSupport channel = this.channel;
    try {
      ReassemblyUtils.addFollowingFrame(
              frames, followingFrame, hasFollows, channel.maxInboundPayloadSize);
    }
    catch (IllegalStateException t) {
      final int streamId = this.streamId;
      channel.remove(streamId, this);

      this.frames = null;
      frames.release();

      final RequestInterceptor requestInterceptor = this.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onTerminate(streamId, FrameType.REQUEST_FNF, t);
      }

      logger.debug("Reassembly has failed", t);
      return;
    }

    if (!hasFollows) {
      channel.remove(this.streamId, this);
      this.frames = null;

      Payload payload;
      try {
        payload = channel.payloadDecoder.decode(frames);
        frames.release();
      }
      catch (Throwable t) {
        ReferenceCountUtil.safeRelease(frames);

        final RequestInterceptor requestInterceptor = this.requestInterceptor;
        if (requestInterceptor != null) {
          requestInterceptor.onTerminate(this.streamId, FrameType.REQUEST_FNF, t);
        }

        logger.debug("Reassembly has failed", t);
        return;
      }

      Mono<Void> source = this.handler.fireAndForget(payload);
      source.subscribe(this);
    }
  }

  @Override
  public final void handleCancel() {
    final CompositeByteBuf frames = this.frames;
    if (frames != null) {
      final int streamId = this.streamId;
      this.channel.remove(streamId, this);

      this.frames = null;
      frames.release();

      final RequestInterceptor requestInterceptor = this.requestInterceptor;
      if (requestInterceptor != null) {
        requestInterceptor.onCancel(streamId, FrameType.REQUEST_FNF);
      }
    }
  }
}
