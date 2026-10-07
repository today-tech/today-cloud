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

import java.util.Arrays;

import infra.remoting.Payload;
import infra.remoting.util.DefaultPayload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

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
