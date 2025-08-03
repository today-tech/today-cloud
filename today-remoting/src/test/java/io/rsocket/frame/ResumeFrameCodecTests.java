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

import java.util.Arrays;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

import static org.assertj.core.api.Assertions.assertThat;

public class ResumeFrameCodecTests {

  @Test
  void testEncoding() {
    byte[] tokenBytes = new byte[65000];
    Arrays.fill(tokenBytes, (byte) 1);
    ByteBuf token = Unpooled.wrappedBuffer(tokenBytes);
    ByteBuf byteBuf = ResumeFrameCodec.encode(ByteBufAllocator.DEFAULT, token, 21, 12);
    assertThat(ResumeFrameCodec.version(byteBuf)).isEqualTo(ResumeFrameCodec.CURRENT_VERSION);
    assertThat(ResumeFrameCodec.token(byteBuf)).isEqualTo(token);
    assertThat(ResumeFrameCodec.lastReceivedServerPos(byteBuf)).isEqualTo(21);
    assertThat(ResumeFrameCodec.firstAvailableClientPos(byteBuf)).isEqualTo(12);
    byteBuf.release();
  }
}
