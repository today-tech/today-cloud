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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

/**
 * Some transports like TCP aren't framed, and require a length. This is used by Connections
 * for transports that need to send length
 */
public class FrameLengthCodec {

  public static final int FRAME_LENGTH_MASK = 0xFFFFFF;
  public static final int FRAME_LENGTH_SIZE = 3;

  private FrameLengthCodec() {
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

  public static ByteBuf encode(ByteBufAllocator allocator, int length, ByteBuf frame) {
    ByteBuf buffer = allocator.buffer();
    encodeLength(buffer, length);
    return allocator.compositeBuffer(2)
            .addComponents(true, buffer, frame);
  }

  public static int length(ByteBuf byteBuf) {
    byteBuf.markReaderIndex();
    int length = decodeLength(byteBuf);
    byteBuf.resetReaderIndex();
    return length;
  }

  public static ByteBuf frame(ByteBuf byteBuf) {
    byteBuf.markReaderIndex();
    byteBuf.skipBytes(3);
    ByteBuf slice = byteBuf.slice();
    byteBuf.resetReaderIndex();
    return slice;
  }
}
