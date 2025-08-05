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

import java.util.function.Consumer;

import infra.remoting.DuplexConnection;
import infra.remoting.Payload;
import infra.remoting.error.CanceledException;
import infra.remoting.frame.CancelFrameCodec;
import infra.remoting.frame.ErrorFrameCodec;
import infra.remoting.frame.FrameType;
import infra.remoting.frame.PayloadFrameCodec;
import infra.remoting.frame.RequestChannelFrameCodec;
import infra.remoting.frame.RequestFireAndForgetFrameCodec;
import infra.remoting.frame.RequestResponseFrameCodec;
import infra.remoting.frame.RequestStreamFrameCodec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.IllegalReferenceCountException;
import io.netty.util.ReferenceCounted;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

import static infra.remoting.core.FragmentationUtils.isFragmentable;

final class SendUtils {

  private static final Consumer<?> DROPPED_ELEMENTS_CONSUMER = data -> {
    if (data instanceof ReferenceCounted rc) {
      try {
        rc.release();
      }
      catch (Throwable e) {
        // ignored
      }
    }
  };

  static final Context DISCARD_CONTEXT = Operators.enableOnDiscard(null, DROPPED_ELEMENTS_CONSUMER);

  static void sendReleasingPayload(int streamId, FrameType frameType, int mtu, Payload payload,
          DuplexConnection connection, ByteBufAllocator allocator, boolean requester) {

    final boolean hasMetadata = payload.hasMetadata();
    final ByteBuf metadata = hasMetadata ? payload.metadata() : null;
    final ByteBuf data = payload.data();

    boolean fragmentable;
    try {
      fragmentable = isFragmentable(mtu, data, metadata, false);
    }
    catch (IllegalReferenceCountException | NullPointerException e) {
      sendTerminalFrame(streamId, frameType, connection, allocator, requester, false, e);
      throw e;
    }

    if (fragmentable) {
      final ByteBuf slicedData = data.slice();
      final ByteBuf slicedMetadata = hasMetadata ? metadata.slice() : Unpooled.EMPTY_BUFFER;

      final ByteBuf first;
      try {
        first = FragmentationUtils.encodeFirstFragment(
                allocator, mtu, frameType, streamId, hasMetadata, slicedMetadata, slicedData);
      }
      catch (IllegalReferenceCountException e) {
        sendTerminalFrame(streamId, frameType, connection, allocator, requester, false, e);
        throw e;
      }

      connection.sendFrame(streamId, first);

      boolean complete = frameType == FrameType.NEXT_COMPLETE;
      while (slicedData.isReadable() || slicedMetadata.isReadable()) {
        final ByteBuf following;
        try {
          following = FragmentationUtils.encodeFollowsFragment(
                  allocator, mtu, streamId, complete, slicedMetadata, slicedData);
        }
        catch (IllegalReferenceCountException e) {
          sendTerminalFrame(streamId, frameType, connection, allocator, requester, true, e);
          throw e;
        }
        connection.sendFrame(streamId, following);
      }

      try {
        payload.release();
      }
      catch (IllegalReferenceCountException e) {
        sendTerminalFrame(streamId, frameType, connection, allocator, true, true, e);
        throw e;
      }
    }
    else {
      final ByteBuf dataRetainedSlice = data.retainedSlice();

      final ByteBuf metadataRetainedSlice;
      try {
        metadataRetainedSlice = hasMetadata ? metadata.retainedSlice() : null;
      }
      catch (IllegalReferenceCountException e) {
        dataRetainedSlice.release();

        sendTerminalFrame(streamId, frameType, connection, allocator, requester, false, e);
        throw e;
      }

      try {
        payload.release();
      }
      catch (IllegalReferenceCountException e) {
        dataRetainedSlice.release();
        if (hasMetadata) {
          metadataRetainedSlice.release();
        }

        sendTerminalFrame(streamId, frameType, connection, allocator, requester, false, e);
        throw e;
      }

      final ByteBuf requestFrame = switch (frameType) {
        case REQUEST_FNF -> RequestFireAndForgetFrameCodec.encode(allocator, streamId, false, metadataRetainedSlice, dataRetainedSlice);
        case REQUEST_RESPONSE -> RequestResponseFrameCodec.encode(allocator, streamId, false, metadataRetainedSlice, dataRetainedSlice);
        case PAYLOAD, NEXT, NEXT_COMPLETE -> PayloadFrameCodec.encode(allocator, streamId, false,
                frameType == FrameType.NEXT_COMPLETE, frameType != FrameType.PAYLOAD, metadataRetainedSlice, dataRetainedSlice);
        default -> throw new IllegalArgumentException("Unsupported frame type " + frameType);
      };

      connection.sendFrame(streamId, requestFrame);
    }
  }

  static void sendReleasingPayload(int streamId, FrameType frameType, long initialRequestN, int mtu,
          Payload payload, DuplexConnection connection, ByteBufAllocator allocator, boolean complete) {

    final boolean hasMetadata = payload.hasMetadata();
    final ByteBuf metadata = hasMetadata ? payload.metadata() : null;
    final ByteBuf data = payload.data();

    boolean fragmentable;
    try {
      fragmentable = isFragmentable(mtu, data, metadata, true);
    }
    catch (IllegalReferenceCountException | NullPointerException e) {
      sendTerminalFrame(streamId, frameType, connection, allocator, true, false, e);
      throw e;
    }

    if (fragmentable) {
      final ByteBuf slicedData = data.slice();
      final ByteBuf slicedMetadata = hasMetadata ? metadata.slice() : Unpooled.EMPTY_BUFFER;

      final ByteBuf first;
      try {
        first = FragmentationUtils.encodeFirstFragment(allocator, mtu, initialRequestN,
                frameType, streamId, hasMetadata, slicedMetadata, slicedData);
      }
      catch (IllegalReferenceCountException e) {
        sendTerminalFrame(streamId, frameType, connection, allocator, true, false, e);
        throw e;
      }

      connection.sendFrame(streamId, first);

      while (slicedData.isReadable() || slicedMetadata.isReadable()) {
        final ByteBuf following;
        try {
          following = FragmentationUtils.encodeFollowsFragment(
                  allocator, mtu, streamId, complete, slicedMetadata, slicedData);
        }
        catch (IllegalReferenceCountException e) {
          sendTerminalFrame(streamId, frameType, connection, allocator, true, true, e);
          throw e;
        }
        connection.sendFrame(streamId, following);
      }

      try {
        payload.release();
      }
      catch (IllegalReferenceCountException e) {
        sendTerminalFrame(streamId, frameType, connection, allocator, true, true, e);
        throw e;
      }
    }
    else {
      final ByteBuf dataRetainedSlice = data.retainedSlice();

      final ByteBuf metadataRetainedSlice;
      try {
        metadataRetainedSlice = hasMetadata ? metadata.retainedSlice() : null;
      }
      catch (IllegalReferenceCountException e) {
        dataRetainedSlice.release();

        sendTerminalFrame(streamId, frameType, connection, allocator, true, false, e);
        throw e;
      }

      try {
        payload.release();
      }
      catch (IllegalReferenceCountException e) {
        dataRetainedSlice.release();
        if (hasMetadata) {
          metadataRetainedSlice.release();
        }

        sendTerminalFrame(streamId, frameType, connection, allocator, true, false, e);
        throw e;
      }

      final ByteBuf requestFrame = switch (frameType) {
        case REQUEST_STREAM -> RequestStreamFrameCodec.encode(allocator, streamId, false, initialRequestN,
                metadataRetainedSlice, dataRetainedSlice);
        case REQUEST_CHANNEL -> RequestChannelFrameCodec.encode(allocator, streamId, false, complete,
                initialRequestN, metadataRetainedSlice, dataRetainedSlice);
        default -> throw new IllegalArgumentException("Unsupported frame type " + frameType);
      };

      connection.sendFrame(streamId, requestFrame);
    }
  }

  static void sendTerminalFrame(int streamId, FrameType frameType, DuplexConnection connection,
          ByteBufAllocator allocator, boolean requester, boolean onFollowingFrame, Throwable t) {

    if (onFollowingFrame) {
      if (requester) {
        final ByteBuf cancelFrame = CancelFrameCodec.encode(allocator, streamId);
        connection.sendFrame(streamId, cancelFrame);
      }
      else {
        final ByteBuf errorFrame = ErrorFrameCodec.encode(allocator, streamId,
                new CanceledException("Failed to encode fragmented %s frame. Cause: %s".formatted(frameType, t.getMessage())));
        connection.sendFrame(streamId, errorFrame);
      }
    }
    else {
      switch (frameType) {
        case NEXT_COMPLETE:
        case NEXT:
        case PAYLOAD:
          if (requester) {
            final ByteBuf cancelFrame = CancelFrameCodec.encode(allocator, streamId);
            connection.sendFrame(streamId, cancelFrame);
          }
          else {
            final ByteBuf errorFrame = ErrorFrameCodec.encode(allocator, streamId, new CanceledException(
                    "Failed to encode %s frame. Cause: %s".formatted(frameType, t.getMessage())));
            connection.sendFrame(streamId, errorFrame);
          }
      }
    }
  }
}
