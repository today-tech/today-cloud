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

package infra.cloud.service;

import org.junit.jupiter.api.Test;
import io.netty.buffer.Unpooled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Metadata wire format and snapshot ownership tests.
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2026/10/5
 */
class MetadataTests {
  @Test
  void roundTripAndBufferIndependentSnapshot() {
    byte[] bytes = { 1, 2 };
    var metadata = new Metadata().add("traceparent", "trace").add("custom", "中文")
            .add("custom", "second").addBinary("bin", bytes);
    bytes[0] = 9;
    var buffer = Unpooled.buffer();
    Metadata decoded;
    try {
      var codec = new DefaultMetadataCodec();
      codec.encode(metadata, buffer);
      decoded = codec.decode(buffer);
      assertThat(buffer.readerIndex()).isZero();
    }
    finally { buffer.release(); }
    assertThat(decoded.get("traceparent")).isEqualTo("trace");
    assertThat(decoded.get("custom")).isEqualTo("中文");
    assertThat(decoded.entries()).hasSize(4);
    assertThat(decoded.getBinary("bin")).containsExactly((byte) 1, (byte) 2);
    decoded.getBinary("bin")[0] = 7;
    assertThat(decoded.getBinary("bin")[0]).isEqualTo((byte) 1);
    assertThatThrownBy(() -> decoded.add("x", "y")).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void compactKnownNamesAndEmptyMetadata() {
    var buffer = Unpooled.buffer();
    try {
      var codec = new DefaultMetadataCodec();
      codec.encode(Metadata.empty(), buffer);
      assertThat(buffer.readableBytes()).isZero();
      codec.encode(new Metadata().add("traceparent", "x"), buffer);
      assertThat(buffer.readableBytes()).isEqualTo(4); // version, tag, length, value
    }
    finally { buffer.release(); }
  }

  @Test
  void rejectsMalformedInputAndLimitsWithoutModifyingOutput() {
    var buffer = Unpooled.buffer();
    try {
      var codec = new DefaultMetadataCodec(8, 1);
      assertThatThrownBy(() -> codec.encode(new Metadata().add("traceparent", "long-value"), buffer))
              .isInstanceOf(IllegalArgumentException.class);
      assertThat(buffer.writerIndex()).isZero();
      buffer.writeByte(1).writeByte(2).writeByte(100);
      assertThatThrownBy(() -> codec.decode(buffer)).hasMessageContaining("Truncated");
      buffer.clear().writeByte(2);
      assertThatThrownBy(() -> codec.decode(buffer)).hasMessageContaining("version");
      buffer.clear().writeByte(1).writeByte(128);
      assertThatThrownBy(() -> codec.decode(buffer)).hasMessageContaining("Truncated");
      buffer.clear().writeByte(1).writeByte(2).writeByte(0).writeByte(2).writeByte(0);
      assertThatThrownBy(() -> codec.decode(buffer)).hasMessageContaining("entries");
    }
    finally { buffer.release(); }
  }
}
