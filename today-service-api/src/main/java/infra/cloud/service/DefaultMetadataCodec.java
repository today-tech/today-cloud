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

import java.nio.charset.StandardCharsets;
import java.util.List;

import infra.util.Assert;
import io.netty.buffer.ByteBuf;

/**
 * Compact V1 codec: version byte, followed by entries until the buffer ends.
 * Entry layout: unsigned-varint tag (name ID shifted left, binary in low bit),
 * optional UTF-8 name length/name for ID zero, then value length/value.
 * IDs 1..5 are traceparent, tracestate, baggage, tenant-id, and authorization.
 * Limits apply symmetrically to encoding and decoding; malformed input is rejected.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2026/10/5
 */
public final class DefaultMetadataCodec implements MetadataCodec {

  private static final List<String> NAMES = List.of("traceparent", "tracestate", "baggage", "tenant-id", "authorization");

  private final int maxBytes;

  private final int maxEntries;

  public DefaultMetadataCodec() {
    this(8192, 64);
  }

  public DefaultMetadataCodec(int maxBytes, int maxEntries) {
    Assert.isTrue(maxBytes > 0 && maxEntries > 0, "Metadata limits must be positive");
    this.maxBytes = maxBytes;
    this.maxEntries = maxEntries;
  }

  @Override
  public void encode(Metadata metadata, ByteBuf output) {
    if (metadata.isEmpty()) {
      return;
    }
    var entries = metadata.entries();
    Assert.isTrue(entries.size() <= maxEntries, "Too many metadata entries");
    // Validate before modifying output, including UTF-8 byte lengths.
    long size = 1;
    for (var entry : entries) {
      int id = NAMES.indexOf(entry.name()) + 1;
      byte[] value = entry.rawValue();
      if (id == 0) {
        int length = entry.name().getBytes(StandardCharsets.UTF_8).length;
        size += varintSize(length) + length;
      }
      size += 1L + varintSize(value.length) + value.length;
    }
    Assert.isTrue(size <= maxBytes, "Metadata exceeds size limit");
    output.writeByte(1);
    for (var entry : entries) {
      int id = NAMES.indexOf(entry.name()) + 1;
      writeVarint(output, (id << 1) | (entry.binary() ? 1 : 0));
      if (id == 0) {
        writeBytes(output, entry.name().getBytes(StandardCharsets.UTF_8));
      }
      writeBytes(output, entry.rawValue());
    }
  }

  @Override
  public Metadata decode(ByteBuf input) {
    if (!input.isReadable()) {
      return Metadata.empty();
    }
    Assert.isTrue(input.readableBytes() <= maxBytes, "Metadata exceeds size limit");
    ByteBuf buffer = input.slice();
    Assert.isTrue(buffer.readUnsignedByte() == 1, "Unsupported metadata version");
    var metadata = new Metadata();
    int count = 0;
    while (buffer.isReadable()) {
      Assert.isTrue(++count <= maxEntries, "Too many metadata entries");
      int tag = readVarint(buffer);
      int id = tag >>> 1;
      Assert.isTrue(id <= NAMES.size(), "Unknown metadata name ID");
      String name = id == 0 ? new String(readBytes(buffer), StandardCharsets.UTF_8) : NAMES.get(id - 1);
      byte[] value = readBytes(buffer);
      if ((tag & 1) != 0) {
        metadata.addBinary(name, value);
      }
      else {
        metadata.add(name, new String(value, StandardCharsets.UTF_8));
      }
    }
    return metadata.snapshot();
  }

  private static void writeBytes(ByteBuf buffer, byte[] bytes) {
    writeVarint(buffer, bytes.length);
    buffer.writeBytes(bytes);
  }

  private static byte[] readBytes(ByteBuf buffer) {
    int length = readVarint(buffer);
    Assert.isTrue(length <= buffer.readableBytes(), "Truncated metadata value");
    byte[] bytes = new byte[length];
    buffer.readBytes(bytes);
    return bytes;
  }

  private static int varintSize(int value) {
    int size = 1;
    while ((value >>>= 7) != 0) { size++; }
    return size;
  }

  private static void writeVarint(ByteBuf buffer, int value) {
    while (value >= 128) {
      buffer.writeByte((value & 127) | 128);
      value >>>= 7;
    }
    buffer.writeByte(value);
  }

  private static int readVarint(ByteBuf buffer) {
    int value = 0;
    for (int shift = 0; shift <= 28; shift += 7) {
      Assert.isTrue(buffer.isReadable(), "Truncated metadata varint");
      int next = buffer.readUnsignedByte();
      Assert.isTrue(shift != 28 || (next & 248) == 0, "Metadata varint overflow");
      value |= (next & 127) << shift;
      if ((next & 128) == 0) {
        return value;
      }
    }
    throw new IllegalArgumentException("Invalid metadata varint");
  }
}
