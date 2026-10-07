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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

public class LeaseFrameCodecTests {

  @Test
  void leaseMetadata() {
    ByteBuf metadata = bytebuf("md");
    int ttl = 1;
    int numRequests = 42;
    ByteBuf lease = LeaseFrameCodec.encode(ByteBufAllocator.DEFAULT, ttl, numRequests, metadata);

    Assertions.assertTrue(FrameHeaderCodec.hasMetadata(lease));
    Assertions.assertEquals(ttl, LeaseFrameCodec.ttl(lease));
    Assertions.assertEquals(numRequests, LeaseFrameCodec.numRequests(lease));
    Assertions.assertEquals(metadata, LeaseFrameCodec.metadata(lease));
    lease.release();
  }

  @Test
  void leaseAbsentMetadata() {
    int ttl = 1;
    int numRequests = 42;
    ByteBuf lease = LeaseFrameCodec.encode(ByteBufAllocator.DEFAULT, ttl, numRequests, null);

    Assertions.assertFalse(FrameHeaderCodec.hasMetadata(lease));
    Assertions.assertEquals(ttl, LeaseFrameCodec.ttl(lease));
    Assertions.assertEquals(numRequests, LeaseFrameCodec.numRequests(lease));
    Assertions.assertNull(LeaseFrameCodec.metadata(lease));
    lease.release();
  }

  private static ByteBuf bytebuf(String str) {
    return Unpooled.copiedBuffer(str, StandardCharsets.UTF_8);
  }
}
