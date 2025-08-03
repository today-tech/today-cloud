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

import infra.lang.Nullable;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public class ExtensionFrameCodec {
  private ExtensionFrameCodec() { }

  public static ByteBuf encode(
          ByteBufAllocator allocator,
          int streamId,
          int extendedType,
          @Nullable ByteBuf metadata,
          ByteBuf data) {

    final boolean hasMetadata = metadata != null;

    int flags = FrameHeaderCodec.FLAGS_I;

    if (hasMetadata) {
      flags |= FrameHeaderCodec.FLAGS_M;
    }

    final ByteBuf header = FrameHeaderCodec.encode(allocator, streamId, FrameType.EXT, flags);
    header.writeInt(extendedType);

    return FrameBodyCodec.encode(allocator, header, metadata, hasMetadata, data);
  }

  public static int extendedType(ByteBuf byteBuf) {
    FrameHeaderCodec.ensureFrameType(FrameType.EXT, byteBuf);
    byteBuf.markReaderIndex();
    byteBuf.skipBytes(FrameHeaderCodec.size());
    int i = byteBuf.readInt();
    byteBuf.resetReaderIndex();
    return i;
  }

  public static ByteBuf data(ByteBuf byteBuf) {
    FrameHeaderCodec.ensureFrameType(FrameType.EXT, byteBuf);

    boolean hasMetadata = FrameHeaderCodec.hasMetadata(byteBuf);
    byteBuf.markReaderIndex();
    // Extended type
    byteBuf.skipBytes(FrameHeaderCodec.size() + Integer.BYTES);
    ByteBuf data = FrameBodyCodec.dataWithoutMarking(byteBuf, hasMetadata);
    byteBuf.resetReaderIndex();
    return data;
  }

  @Nullable
  public static ByteBuf metadata(ByteBuf byteBuf) {
    FrameHeaderCodec.ensureFrameType(FrameType.EXT, byteBuf);

    boolean hasMetadata = FrameHeaderCodec.hasMetadata(byteBuf);
    if (!hasMetadata) {
      return null;
    }
    byteBuf.markReaderIndex();
    // Extended type
    byteBuf.skipBytes(FrameHeaderCodec.size() + Integer.BYTES);
    ByteBuf metadata = FrameBodyCodec.metadataWithoutMarking(byteBuf);
    byteBuf.resetReaderIndex();
    return metadata;
  }
}
