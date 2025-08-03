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

package infra.remoting.frame;

import infra.remoting.Payload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.IllegalReferenceCountException;

public class MetadataPushFrameCodec {

  public static ByteBuf encodeReleasingPayload(ByteBufAllocator allocator, Payload payload) {
    if (!payload.hasMetadata()) {
      throw new IllegalStateException(
              "Metadata  push requires to have metadata present" + " in the given Payload");
    }
    final ByteBuf metadata = payload.metadata().retain();
    // releasing payload safely since it can be already released wheres we have to release retained
    // data and metadata as well
    try {
      payload.release();
    }
    catch (IllegalReferenceCountException e) {
      metadata.release();
      throw e;
    }
    return encode(allocator, metadata);
  }

  public static ByteBuf encode(ByteBufAllocator allocator, ByteBuf metadata) {
    ByteBuf header =
            FrameHeaderCodec.encodeStreamZero(
                    allocator, FrameType.METADATA_PUSH, FrameHeaderCodec.FLAGS_M);
    return allocator.compositeBuffer(2).addComponents(true, header, metadata);
  }

  public static ByteBuf metadata(ByteBuf byteBuf) {
    byteBuf.markReaderIndex();
    int headerSize = FrameHeaderCodec.size();
    int metadataLength = byteBuf.readableBytes() - headerSize;
    byteBuf.skipBytes(headerSize);
    ByteBuf metadata = byteBuf.readSlice(metadataLength);
    byteBuf.resetReaderIndex();
    return metadata;
  }
}
