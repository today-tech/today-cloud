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

import infra.remoting.Payload;
import infra.remoting.frame.FrameHeaderCodec;
import infra.remoting.frame.FrameLengthCodec;
import infra.remoting.frame.FrameType;
import infra.remoting.frame.decoder.PayloadDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.util.IllegalReferenceCountException;
import io.netty.util.ReferenceCountUtil;
import reactor.core.CoreSubscriber;

import static infra.remoting.core.FragmentationUtils.MIN_MTU_SIZE;
import static infra.remoting.core.StateUtils.isReassembling;
import static infra.remoting.core.StateUtils.isTerminated;
import static infra.remoting.core.StateUtils.markReassembled;
import static infra.remoting.core.StateUtils.markReassembling;
import static infra.remoting.frame.FrameLengthCodec.FRAME_LENGTH_MASK;

class ReassemblyUtils {
  static final String ILLEGAL_REASSEMBLED_PAYLOAD_SIZE =
          "Reassembled payload size went out of allowed %s bytes";

  @SuppressWarnings("ConstantConditions")
  static void release(RequesterFrameHandler framesHolder, long state) {
    if (isReassembling(state)) {
      final CompositeByteBuf frames = framesHolder.getFrames();
      framesHolder.setFrames(null);
      frames.release();
    }
  }

  @SuppressWarnings({ "ConstantConditions", "SynchronizationOnLocalVariableOrMethodParameter" })
  static void synchronizedRelease(RequesterFrameHandler framesHolder, long state) {
    if (isReassembling(state)) {
      final CompositeByteBuf frames = framesHolder.getFrames();
      framesHolder.setFrames(null);

      synchronized(frames) {
        frames.release();
      }
    }
  }

  static <T extends RequesterFrameHandler> void handleNextSupport(
          AtomicLongFieldUpdater<T> updater,
          T instance,
          Subscription subscription,
          CoreSubscriber<? super Payload> inboundSubscriber,
          PayloadDecoder payloadDecoder,
          ByteBufAllocator allocator,
          int maxInboundPayloadSize,
          ByteBuf frame,
          boolean hasFollows,
          boolean isLastPayload) {

    long state = updater.get(instance);
    if (isTerminated(state)) {
      return;
    }

    if (!hasFollows && !isReassembling(state)) {
      Payload payload;
      try {
        payload = payloadDecoder.decode(frame);
      }
      catch (Throwable t) {
        // sends cancel frame to prevent any further frames
        subscription.cancel();
        // terminates downstream
        inboundSubscriber.onError(t);

        return;
      }

      instance.handlePayload(payload);
      if (isLastPayload) {
        instance.handleComplete();
      }
      return;
    }

    CompositeByteBuf frames = instance.getFrames();
    if (frames == null) {
      frames =
              ReassemblyUtils.addFollowingFrame(
                      allocator.compositeBuffer(), frame, hasFollows, maxInboundPayloadSize);
      instance.setFrames(frames);

      long previousState = markReassembling(updater, instance);
      if (isTerminated(previousState)) {
        instance.setFrames(null);
        frames.release();
        return;
      }
    }
    else {
      try {
        frames =
                ReassemblyUtils.addFollowingFrame(frames, frame, hasFollows, maxInboundPayloadSize);
      }
      catch (IllegalStateException t) {
        if (isTerminated(updater.get(instance))) {
          return;
        }

        // sends cancel frame to prevent any further frames
        subscription.cancel();
        // terminates downstream
        inboundSubscriber.onError(t);

        return;
      }
    }

    if (!hasFollows) {
      long previousState = markReassembled(updater, instance);
      if (isTerminated(previousState)) {
        return;
      }

      instance.setFrames(null);

      Payload payload;
      try {
        payload = payloadDecoder.decode(frames);
        frames.release();
      }
      catch (Throwable t) {
        ReferenceCountUtil.safeRelease(frames);

        // sends cancel frame to prevent any further frames
        subscription.cancel();
        // terminates downstream
        inboundSubscriber.onError(t);

        return;
      }

      instance.handlePayload(payload);

      if (isLastPayload) {
        instance.handleComplete();
      }
    }
  }

  static CompositeByteBuf addFollowingFrame(
          CompositeByteBuf frames,
          ByteBuf followingFrame,
          boolean hasFollows,
          int maxInboundPayloadSize) {
    int readableBytes = frames.readableBytes();
    if (readableBytes == 0) {
      return frames.addComponent(true, followingFrame.retain());
    }
    else if (maxInboundPayloadSize != Integer.MAX_VALUE
            && readableBytes + followingFrame.readableBytes() - FrameHeaderCodec.size()
            > maxInboundPayloadSize) {
      throw new IllegalStateException(
              String.format(ILLEGAL_REASSEMBLED_PAYLOAD_SIZE, maxInboundPayloadSize));
    }
    else if (followingFrame.readableBytes() < MIN_MTU_SIZE - 3 && hasFollows) {
      // FIXME: check MIN_MTU_SIZE only (currently fragments have size of 61)
      throw new IllegalStateException("Fragment is too small.");
    }

    final boolean hasMetadata = FrameHeaderCodec.hasMetadata(followingFrame);

    // skip headers
    followingFrame.skipBytes(FrameHeaderCodec.size());

    // if has metadata, then we have to increase metadata length in containing frames
    // CompositeByteBuf
    if (hasMetadata) {
      final FrameType frameType = FrameHeaderCodec.frameType(frames);
      final int lengthFieldPosition =
              FrameHeaderCodec.size() + (frameType.hasInitialRequestN() ? Integer.BYTES : 0);

      frames.markReaderIndex();
      frames.skipBytes(lengthFieldPosition);

      final int nextMetadataLength = decodeLength(frames) + decodeLength(followingFrame);

      frames.resetReaderIndex();

      frames.markWriterIndex();
      frames.writerIndex(lengthFieldPosition);

      encodeLength(frames, nextMetadataLength);

      frames.resetWriterIndex();
    }

    synchronized(frames) {
      if (frames.refCnt() > 0) {
        followingFrame.retain();
        return frames.addComponent(true, followingFrame);
      }
      else {
        throw new IllegalReferenceCountException(0);
      }
    }
  }

  private static void encodeLength(final ByteBuf byteBuf, final int length) {
    if ((length & ~FRAME_LENGTH_MASK) != 0) {
      throw new IllegalArgumentException("Length is larger than 24 bits");
    }
    // Write each byte separately in reverse order, this mean we can write 1 << 23 without
    // overflowing.
    byteBuf.writeByte(length >> 16);
    byteBuf.writeByte(length >> 8);
    byteBuf.writeByte(length);
  }

  private static int decodeLength(final ByteBuf byteBuf) {
    int length = (byteBuf.readByte() & 0xFF) << 16;
    length |= (byteBuf.readByte() & 0xFF) << 8;
    length |= byteBuf.readByte() & 0xFF;
    return length;
  }

  static int assertInboundPayloadSize(int inboundPayloadSize) {
    if (inboundPayloadSize < MIN_MTU_SIZE) {
      String msg =
              String.format(
                      "The min allowed inboundPayloadSize size is %d bytes, provided: %d",
                      FrameLengthCodec.FRAME_LENGTH_MASK, inboundPayloadSize);
      throw new IllegalArgumentException(msg);
    }
    else {
      return inboundPayloadSize;
    }
  }
}
