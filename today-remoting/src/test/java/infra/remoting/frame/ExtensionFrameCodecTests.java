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
