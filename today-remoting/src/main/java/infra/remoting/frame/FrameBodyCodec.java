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
import io.netty.buffer.Unpooled;

class FrameBodyCodec {
  public static final int FRAME_LENGTH_MASK = 0xFFFFFF;

  private FrameBodyCodec() {
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
    byte b = byteBuf.readByte();
    int length = (b & 0xFF) << 16;
    byte b1 = byteBuf.readByte();
    length |= (b1 & 0xFF) << 8;
    byte b2 = byteBuf.readByte();
    length |= b2 & 0xFF;
    return length;
  }

  static ByteBuf encode(ByteBufAllocator allocator, final ByteBuf header,
          @Nullable ByteBuf metadata, boolean hasMetadata, @Nullable ByteBuf data) {

    final boolean addData;
    if (data != null) {
      if (data.isReadable()) {
        addData = true;
      }
      else {
        // even though there is nothing to read, we still have to release here since nobody else
        // going to do soo
        data.release();
        addData = false;
      }
    }
    else {
      addData = false;
    }

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

    if (hasMetadata) {
      int length = metadata.readableBytes();
      encodeLength(header, length);
    }

    if (addMetadata && addData) {
      return allocator.compositeBuffer(3).addComponents(true, header, metadata, data);
    }
    else if (addMetadata) {
      return allocator.compositeBuffer(2).addComponents(true, header, metadata);
    }
    else if (addData) {
      return allocator.compositeBuffer(2).addComponents(true, header, data);
    }
    else {
      return header;
    }
  }

  static ByteBuf metadataWithoutMarking(ByteBuf byteBuf) {
    int length = decodeLength(byteBuf);
    return byteBuf.readSlice(length);
  }

  static ByteBuf dataWithoutMarking(ByteBuf byteBuf, boolean hasMetadata) {
    if (hasMetadata) {
      /*moves reader index*/
      int length = decodeLength(byteBuf);
      byteBuf.skipBytes(length);
    }
    if (byteBuf.readableBytes() > 0) {
      return byteBuf.readSlice(byteBuf.readableBytes());
    }
    else {
      return Unpooled.EMPTY_BUFFER;
    }
  }
}
