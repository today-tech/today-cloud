/*
 * Copyright 2021 - 2026 the TODAY authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.remoting.frame;

import org.jspecify.annotations.Nullable;

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
