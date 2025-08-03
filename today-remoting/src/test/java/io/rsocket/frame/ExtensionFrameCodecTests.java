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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

public class ExtensionFrameCodecTests {

  @Test
  void extensionDataMetadata() {
    ByteBuf metadata = bytebuf("md");
    ByteBuf data = bytebuf("d");
    int extendedType = 1;

    ByteBuf extension =
            ExtensionFrameCodec.encode(ByteBufAllocator.DEFAULT, 1, extendedType, metadata, data);

    Assertions.assertTrue(FrameHeaderCodec.hasMetadata(extension));
    Assertions.assertEquals(extendedType, ExtensionFrameCodec.extendedType(extension));
    Assertions.assertEquals(metadata, ExtensionFrameCodec.metadata(extension));
    Assertions.assertEquals(data, ExtensionFrameCodec.data(extension));
    extension.release();
  }

  @Test
  void extensionData() {
    ByteBuf data = bytebuf("d");
    int extendedType = 1;

    ByteBuf extension =
            ExtensionFrameCodec.encode(ByteBufAllocator.DEFAULT, 1, extendedType, null, data);

    Assertions.assertFalse(FrameHeaderCodec.hasMetadata(extension));
    Assertions.assertEquals(extendedType, ExtensionFrameCodec.extendedType(extension));
    Assertions.assertNull(ExtensionFrameCodec.metadata(extension));
    Assertions.assertEquals(data, ExtensionFrameCodec.data(extension));
    extension.release();
  }

  @Test
  void extensionMetadata() {
    ByteBuf metadata = bytebuf("md");
    int extendedType = 1;

    ByteBuf extension =
            ExtensionFrameCodec.encode(
                    ByteBufAllocator.DEFAULT, 1, extendedType, metadata, Unpooled.EMPTY_BUFFER);

    Assertions.assertTrue(FrameHeaderCodec.hasMetadata(extension));
    Assertions.assertEquals(extendedType, ExtensionFrameCodec.extendedType(extension));
    Assertions.assertEquals(metadata, ExtensionFrameCodec.metadata(extension));
    Assertions.assertEquals(0, ExtensionFrameCodec.data(extension).readableBytes());
    extension.release();
  }

  private static ByteBuf bytebuf(String str) {
    return Unpooled.copiedBuffer(str, StandardCharsets.UTF_8);
  }
}
