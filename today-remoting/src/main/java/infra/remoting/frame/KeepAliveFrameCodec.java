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

public class KeepAliveFrameCodec {
  /**
   * (R)espond: Set by the sender of the KEEPALIVE, to which the responder MUST reply with a
   * KEEPALIVE without the R flag set
   */
  public static final int FLAGS_KEEPALIVE_R = 0b00_1000_0000;

  public static final long LAST_POSITION_MASK = 0x8000000000000000L;

  private KeepAliveFrameCodec() {
  }

  public static ByteBuf encode(final ByteBufAllocator allocator,
          final boolean respond, final long lastPosition, final ByteBuf data) {
    final int flags = respond ? FLAGS_KEEPALIVE_R : 0;
    ByteBuf header = FrameHeaderCodec.encodeStreamZero(allocator, FrameType.KEEPALIVE, flags);

    long lp = 0;
    if (lastPosition > 0) {
      lp |= lastPosition;
    }

    header.writeLong(lp);

    return FrameBodyCodec.encode(allocator, header, null, false, data);
  }

  public static boolean respondFlag(ByteBuf byteBuf) {
    FrameHeaderCodec.ensureFrameType(FrameType.KEEPALIVE, byteBuf);
    int flags = FrameHeaderCodec.flags(byteBuf);
    return (flags & FLAGS_KEEPALIVE_R) == FLAGS_KEEPALIVE_R;
  }

  public static long lastPosition(ByteBuf byteBuf) {
    FrameHeaderCodec.ensureFrameType(FrameType.KEEPALIVE, byteBuf);
    byteBuf.markReaderIndex();
    long l = byteBuf.skipBytes(FrameHeaderCodec.size()).readLong();
    byteBuf.resetReaderIndex();
    return l;
  }

  public static ByteBuf data(ByteBuf byteBuf) {
    FrameHeaderCodec.ensureFrameType(FrameType.KEEPALIVE, byteBuf);
    byteBuf.markReaderIndex();
    ByteBuf slice = byteBuf.skipBytes(FrameHeaderCodec.size() + Long.BYTES).slice();
    byteBuf.resetReaderIndex();
    return slice;
  }
}
