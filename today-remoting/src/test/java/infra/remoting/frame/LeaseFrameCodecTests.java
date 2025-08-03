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
