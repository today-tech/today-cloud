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

import java.util.Arrays;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import infra.remoting.Payload;
import infra.remoting.util.DefaultPayload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetupFrameCodecTests {
  @Test
  void testEncodingNoResume() {
    ByteBuf metadata = Unpooled.wrappedBuffer(new byte[] { 1, 2, 3, 4 });
    ByteBuf data = Unpooled.wrappedBuffer(new byte[] { 5, 4, 3 });
    Payload payload = DefaultPayload.create(data, metadata);
    ByteBuf frame =
            SetupFrameCodec.encode(
                    ByteBufAllocator.DEFAULT, false, 5, 500, "metadata_type", "data_type", payload);

    assertEquals(FrameType.SETUP, FrameHeaderCodec.frameType(frame));
    assertFalse(SetupFrameCodec.resumeEnabled(frame));
    assertEquals(0, SetupFrameCodec.resumeToken(frame).readableBytes());
    assertEquals("metadata_type", SetupFrameCodec.metadataMimeType(frame));
    assertEquals("data_type", SetupFrameCodec.dataMimeType(frame));
    assertEquals(payload.metadata(), SetupFrameCodec.metadata(frame));
    assertEquals(payload.data(), SetupFrameCodec.data(frame));
    assertEquals(SetupFrameCodec.CURRENT_VERSION, SetupFrameCodec.version(frame));
    frame.release();
  }

  @Test
  void testEncodingResume() {
    byte[] tokenBytes = new byte[65000];
    Arrays.fill(tokenBytes, (byte) 1);
    ByteBuf metadata = Unpooled.wrappedBuffer(new byte[] { 1, 2, 3, 4 });
    ByteBuf data = Unpooled.wrappedBuffer(new byte[] { 5, 4, 3 });
    Payload payload = DefaultPayload.create(data, metadata);
    ByteBuf token = Unpooled.wrappedBuffer(tokenBytes);
    ByteBuf frame =
            SetupFrameCodec.encode(
                    ByteBufAllocator.DEFAULT, true, 5, 500, token, "metadata_type", "data_type", payload);

    assertEquals(FrameType.SETUP, FrameHeaderCodec.frameType(frame));
    assertTrue(SetupFrameCodec.honorLease(frame));
    assertTrue(SetupFrameCodec.resumeEnabled(frame));
    assertEquals(token, SetupFrameCodec.resumeToken(frame));
    assertEquals("metadata_type", SetupFrameCodec.metadataMimeType(frame));
    assertEquals("data_type", SetupFrameCodec.dataMimeType(frame));
    assertEquals(payload.metadata(), SetupFrameCodec.metadata(frame));
    assertEquals(payload.data(), SetupFrameCodec.data(frame));
    assertEquals(SetupFrameCodec.CURRENT_VERSION, SetupFrameCodec.version(frame));
    frame.release();
  }
}
