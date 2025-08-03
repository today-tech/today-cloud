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

import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class FrameHeaderCodecTests {
  // Taken from spec
  private static final int FRAME_MAX_SIZE = 16_777_215;

  @Test
  void typeAndFlag() {
    FrameType frameType = FrameType.REQUEST_FNF;
    int flags = 0b1110110111;
    ByteBuf header = FrameHeaderCodec.encode(ByteBufAllocator.DEFAULT, 0, frameType, flags);

    assertEquals(flags, FrameHeaderCodec.flags(header));
    assertEquals(frameType, FrameHeaderCodec.frameType(header));
    header.release();
  }

  @Test
  void typeAndFlagTruncated() {
    FrameType frameType = FrameType.SETUP;
    int flags = 0b11110110111; // 1 bit too many
    ByteBuf header = FrameHeaderCodec.encode(ByteBufAllocator.DEFAULT, 0, frameType, flags);

    assertNotEquals(flags, FrameHeaderCodec.flags(header));
    assertEquals(flags & 0b0000_0011_1111_1111, FrameHeaderCodec.flags(header));
    assertEquals(frameType, FrameHeaderCodec.frameType(header));
    header.release();
  }
}
