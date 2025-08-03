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
import io.netty.buffer.ByteBufUtil;
import infra.remoting.exceptions.ApplicationErrorException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorFrameCodecTests {
  @Test
  void testEncode() {
    ByteBuf frame =
            ErrorFrameCodec.encode(ByteBufAllocator.DEFAULT, 1, new ApplicationErrorException("d"));

    frame = FrameLengthCodec.encode(ByteBufAllocator.DEFAULT, frame.readableBytes(), frame);
    assertEquals("00000b000000012c000000020164", ByteBufUtil.hexDump(frame));
    frame.release();
  }
}
