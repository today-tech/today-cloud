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

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeepaliveFrameFlyweightTests {
  @Test
  void canReadData() {
    ByteBuf data = Unpooled.wrappedBuffer(new byte[] { 5, 4, 3 });
    ByteBuf frame = KeepAliveFrameCodec.encode(ByteBufAllocator.DEFAULT, true, 0, data);
    assertTrue(KeepAliveFrameCodec.respondFlag(frame));
    assertEquals(data, KeepAliveFrameCodec.data(frame));
    frame.release();
  }

  @Test
  void testEncoding() {
    ByteBuf frame =
            KeepAliveFrameCodec.encode(
                    ByteBufAllocator.DEFAULT, true, 0, Unpooled.copiedBuffer("d", StandardCharsets.UTF_8));
    frame = FrameLengthCodec.encode(ByteBufAllocator.DEFAULT, frame.readableBytes(), frame);
    assertEquals("00000f000000000c80000000000000000064", ByteBufUtil.hexDump(frame));
    frame.release();
  }
}
