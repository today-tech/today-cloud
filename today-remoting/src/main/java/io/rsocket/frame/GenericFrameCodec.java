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

package io.rsocket.frame;

import infra.lang.Nullable;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.IllegalReferenceCountException;
import io.rsocket.Payload;

class GenericFrameCodec {

  static ByteBuf encodeReleasingPayload(
          final ByteBufAllocator allocator,
          final FrameType frameType,
          final int streamId,
          boolean complete,
          boolean next,
          final Payload payload) {
    return encodeReleasingPayload(allocator, frameType, streamId, complete, next, 0, payload);
  }

  static ByteBuf encodeReleasingPayload(
          final ByteBufAllocator allocator,
          final FrameType frameType,
          final int streamId,
          boolean complete,
          boolean next,
          int requestN,
          final Payload payload) {

    // if refCnt exceptions throws here it is safe to do no-op
    boolean hasMetadata = payload.hasMetadata();
    // if refCnt exceptions throws here it is safe to do no-op still
    final ByteBuf metadata = hasMetadata ? payload.metadata().retain() : null;
    final ByteBuf data;
    // retaining data safely. May throw either NPE or RefCntE
    try {
      data = payload.data().retain();
    }
    catch (IllegalReferenceCountException | NullPointerException e) {
      if (hasMetadata) {
        metadata.release();
      }
      throw e;
    }
    // releasing payload safely since it can be already released wheres we have to release retained
    // data and metadata as well
    try {
      payload.release();
    }
    catch (IllegalReferenceCountException e) {
      data.release();
      if (hasMetadata) {
        metadata.release();
      }
      throw e;
    }

    return encode(allocator, frameType, streamId, false, complete, next, requestN, metadata, data);
  }

  static ByteBuf encode(
          final ByteBufAllocator allocator,
          final FrameType frameType,
          final int streamId,
          boolean fragmentFollows,
          @Nullable ByteBuf metadata,
          ByteBuf data) {
    return encode(allocator, frameType, streamId, fragmentFollows, false, false, 0, metadata, data);
  }

  static ByteBuf encode(
          final ByteBufAllocator allocator,
          final FrameType frameType,
          final int streamId,
          boolean fragmentFollows,
          boolean complete,
          boolean next,
          int requestN,
          @Nullable ByteBuf metadata,
          @Nullable ByteBuf data) {

    final boolean hasMetadata = metadata != null;

    int flags = 0;

    if (hasMetadata) {
      flags |= FrameHeaderCodec.FLAGS_M;
    }

    if (fragmentFollows) {
      flags |= FrameHeaderCodec.FLAGS_F;
    }

    if (complete) {
      flags |= FrameHeaderCodec.FLAGS_C;
    }

    if (next) {
      flags |= FrameHeaderCodec.FLAGS_N;
    }

    final ByteBuf header = FrameHeaderCodec.encode(allocator, streamId, frameType, flags);

    if (requestN > 0) {
      header.writeInt(requestN);
    }

    return FrameBodyCodec.encode(allocator, header, metadata, hasMetadata, data);
  }

  static ByteBuf data(ByteBuf byteBuf) {
    boolean hasMetadata = FrameHeaderCodec.hasMetadata(byteBuf);
    int idx = byteBuf.readerIndex();
    byteBuf.skipBytes(FrameHeaderCodec.size());
    ByteBuf data = FrameBodyCodec.dataWithoutMarking(byteBuf, hasMetadata);
    byteBuf.readerIndex(idx);
    return data;
  }

  @Nullable
  static ByteBuf metadata(ByteBuf byteBuf) {
    boolean hasMetadata = FrameHeaderCodec.hasMetadata(byteBuf);
    if (!hasMetadata) {
      return null;
    }
    byteBuf.markReaderIndex();
    byteBuf.skipBytes(FrameHeaderCodec.size());
    ByteBuf metadata = FrameBodyCodec.metadataWithoutMarking(byteBuf);
    byteBuf.resetReaderIndex();
    return metadata;
  }

  static ByteBuf dataWithRequestN(ByteBuf byteBuf) {
    boolean hasMetadata = FrameHeaderCodec.hasMetadata(byteBuf);
    byteBuf.markReaderIndex();
    byteBuf.skipBytes(FrameHeaderCodec.size() + Integer.BYTES);
    ByteBuf data = FrameBodyCodec.dataWithoutMarking(byteBuf, hasMetadata);
    byteBuf.resetReaderIndex();
    return data;
  }

  @Nullable
  static ByteBuf metadataWithRequestN(ByteBuf byteBuf) {
    boolean hasMetadata = FrameHeaderCodec.hasMetadata(byteBuf);
    if (!hasMetadata) {
      return null;
    }
    byteBuf.markReaderIndex();
    byteBuf.skipBytes(FrameHeaderCodec.size() + Integer.BYTES);
    ByteBuf metadata = FrameBodyCodec.metadataWithoutMarking(byteBuf);
    byteBuf.resetReaderIndex();
    return metadata;
  }

  static int initialRequestN(ByteBuf byteBuf) {
    byteBuf.markReaderIndex();
    int i = byteBuf.skipBytes(FrameHeaderCodec.size()).readInt();
    byteBuf.resetReaderIndex();
    return i;
  }
}
