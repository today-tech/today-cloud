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

public class LeaseFrameCodec {

  public static ByteBuf encode(
          final ByteBufAllocator allocator,
          final int ttl,
          final int numRequests,
          @Nullable final ByteBuf metadata) {

    final boolean hasMetadata = metadata != null;

    int flags = 0;

    if (hasMetadata) {
      flags |= FrameHeaderCodec.FLAGS_M;
    }

    final ByteBuf header =
            FrameHeaderCodec.encodeStreamZero(allocator, FrameType.LEASE, flags)
                    .writeInt(ttl)
                    .writeInt(numRequests);

    final boolean addMetadata;
    if (hasMetadata) {
      if (metadata.isReadable()) {
        addMetadata = true;
      }
      else {
        // even though there is nothing to read, we still have to release here since nobody else
        // going to do soo
        metadata.release();
        addMetadata = false;
      }
    }
    else {
      // has no metadata means it is null, thus no need to release anything
      addMetadata = false;
    }

    if (addMetadata) {
      return allocator.compositeBuffer(2).addComponents(true, header, metadata);
    }
    else {
      return header;
    }
  }

  public static int ttl(final ByteBuf byteBuf) {
    FrameHeaderCodec.ensureFrameType(FrameType.LEASE, byteBuf);
    byteBuf.markReaderIndex();
    byteBuf.skipBytes(FrameHeaderCodec.size());
    int ttl = byteBuf.readInt();
    byteBuf.resetReaderIndex();
    return ttl;
  }

  public static int numRequests(final ByteBuf byteBuf) {
    FrameHeaderCodec.ensureFrameType(FrameType.LEASE, byteBuf);
    byteBuf.markReaderIndex();
    // Ttl
    byteBuf.skipBytes(FrameHeaderCodec.size() + Integer.BYTES);
    int numRequests = byteBuf.readInt();
    byteBuf.resetReaderIndex();
    return numRequests;
  }

  @Nullable
  public static ByteBuf metadata(final ByteBuf byteBuf) {
    FrameHeaderCodec.ensureFrameType(FrameType.LEASE, byteBuf);
    if (FrameHeaderCodec.hasMetadata(byteBuf)) {
      byteBuf.markReaderIndex();
      // Ttl + Num of requests
      byteBuf.skipBytes(FrameHeaderCodec.size() + Integer.BYTES * 2);
      ByteBuf metadata = byteBuf.slice();
      byteBuf.resetReaderIndex();
      return metadata;
    }
    else {
      return null;
    }
  }
}
