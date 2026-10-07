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
