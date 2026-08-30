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

import infra.remoting.Payload;
import infra.remoting.util.DefaultPayload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

public class PayloadFlyweightTests {

  @Test
  void nextCompleteDataMetadata() {
    Payload payload = DefaultPayload.create("d", "md");
    ByteBuf nextComplete =
            PayloadFrameCodec.encodeNextCompleteReleasingPayload(ByteBufAllocator.DEFAULT, 1, payload);
    String data = PayloadFrameCodec.data(nextComplete).toString(StandardCharsets.UTF_8);
    String metadata = PayloadFrameCodec.metadata(nextComplete).toString(StandardCharsets.UTF_8);
    Assertions.assertEquals("d", data);
    Assertions.assertEquals("md", metadata);
    nextComplete.release();
  }

  @Test
  void nextCompleteData() {
    Payload payload = DefaultPayload.create("d");
    ByteBuf nextComplete =
            PayloadFrameCodec.encodeNextCompleteReleasingPayload(ByteBufAllocator.DEFAULT, 1, payload);
    String data = PayloadFrameCodec.data(nextComplete).toString(StandardCharsets.UTF_8);
    ByteBuf metadata = PayloadFrameCodec.metadata(nextComplete);
    Assertions.assertEquals("d", data);
    Assertions.assertNull(metadata);
    nextComplete.release();
  }

  @Test
  void nextCompleteMetaData() {
    Payload payload =
            DefaultPayload.create(
                    Unpooled.EMPTY_BUFFER, Unpooled.wrappedBuffer("md".getBytes(StandardCharsets.UTF_8)));

    ByteBuf nextComplete =
            PayloadFrameCodec.encodeNextCompleteReleasingPayload(ByteBufAllocator.DEFAULT, 1, payload);
    ByteBuf data = PayloadFrameCodec.data(nextComplete);
    String metadata = PayloadFrameCodec.metadata(nextComplete).toString(StandardCharsets.UTF_8);
    Assertions.assertTrue(data.readableBytes() == 0);
    Assertions.assertEquals("md", metadata);
    nextComplete.release();
  }

  @Test
  void nextDataMetadata() {
    Payload payload = DefaultPayload.create("d", "md");
    ByteBuf next =
            PayloadFrameCodec.encodeNextReleasingPayload(ByteBufAllocator.DEFAULT, 1, payload);
    String data = PayloadFrameCodec.data(next).toString(StandardCharsets.UTF_8);
    String metadata = PayloadFrameCodec.metadata(next).toString(StandardCharsets.UTF_8);
    Assertions.assertEquals("d", data);
    Assertions.assertEquals("md", metadata);
    next.release();
  }

  @Test
  void nextData() {
    Payload payload = DefaultPayload.create("d");
    ByteBuf next =
            PayloadFrameCodec.encodeNextReleasingPayload(ByteBufAllocator.DEFAULT, 1, payload);
    String data = PayloadFrameCodec.data(next).toString(StandardCharsets.UTF_8);
    ByteBuf metadata = PayloadFrameCodec.metadata(next);
    Assertions.assertEquals("d", data);
    Assertions.assertNull(metadata);
    next.release();
  }

  @Test
  void nextDataEmptyMetadata() {
    Payload payload = DefaultPayload.create("d".getBytes(), new byte[0]);
    ByteBuf next =
            PayloadFrameCodec.encodeNextReleasingPayload(ByteBufAllocator.DEFAULT, 1, payload);
    String data = PayloadFrameCodec.data(next).toString(StandardCharsets.UTF_8);
    ByteBuf metadata = PayloadFrameCodec.metadata(next);
    Assertions.assertEquals("d", data);
    Assertions.assertEquals(metadata.readableBytes(), 0);
    next.release();
  }
}
