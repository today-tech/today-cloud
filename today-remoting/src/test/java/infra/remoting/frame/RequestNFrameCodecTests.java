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
import io.netty.buffer.ByteBufUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestNFrameCodecTests {
  @Test
  void testEncoding() {
    ByteBuf frame = RequestNFrameCodec.encode(ByteBufAllocator.DEFAULT, 1, 5);

    frame = FrameLengthCodec.encode(ByteBufAllocator.DEFAULT, frame.readableBytes(), frame);
    assertEquals("00000a00000001200000000005", ByteBufUtil.hexDump(frame));
    frame.release();
  }
}
