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
