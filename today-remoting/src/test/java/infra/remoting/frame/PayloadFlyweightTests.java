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
import infra.remoting.Payload;
import infra.remoting.util.DefaultPayload;

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
