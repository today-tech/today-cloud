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

package infra.remoting.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import infra.lang.Nullable;
import infra.remoting.Payload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * An implementation of {@link Payload}. This implementation is <b>not</b> thread-safe, and hence
 * any method can not be invoked concurrently.
 */
public final class DefaultPayload implements Payload {
  public static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocateDirect(0);

  private final ByteBuffer data;
  private final ByteBuffer metadata;

  private DefaultPayload(ByteBuffer data, @Nullable ByteBuffer metadata) {
    this.data = data;
    this.metadata = metadata;
  }

  /**
   * Static factory method for a text payload. Mainly looks better than "new DefaultPayload(data)"
   *
   * @param data the data of the payload.
   * @return a payload.
   */
  public static Payload create(CharSequence data) {
    return create(StandardCharsets.UTF_8.encode(CharBuffer.wrap(data)), null);
  }

  /**
   * Static factory method for a text payload. Mainly looks better than "new DefaultPayload(data,
   * metadata)"
   *
   * @param data the data of the payload.
   * @param metadata the metadata for the payload.
   * @return a payload.
   */
  public static Payload create(CharSequence data, @Nullable CharSequence metadata) {
    return create(
            StandardCharsets.UTF_8.encode(CharBuffer.wrap(data)),
            metadata == null ? null : StandardCharsets.UTF_8.encode(CharBuffer.wrap(metadata)));
  }

  public static Payload create(CharSequence data, Charset dataCharset) {
    return create(dataCharset.encode(CharBuffer.wrap(data)), null);
  }

  public static Payload create(
          CharSequence data,
          Charset dataCharset,
          @Nullable CharSequence metadata,
          Charset metadataCharset) {
    return create(
            dataCharset.encode(CharBuffer.wrap(data)),
            metadata == null ? null : metadataCharset.encode(CharBuffer.wrap(metadata)));
  }

  public static Payload create(byte[] data) {
    return create(ByteBuffer.wrap(data), null);
  }

  public static Payload create(byte[] data, @Nullable byte[] metadata) {
    return create(ByteBuffer.wrap(data), metadata == null ? null : ByteBuffer.wrap(metadata));
  }

  public static Payload create(ByteBuffer data) {
    return create(data, null);
  }

  public static Payload create(ByteBuffer data, @Nullable ByteBuffer metadata) {
    return new DefaultPayload(data, metadata);
  }

  public static Payload create(ByteBuf data) {
    return create(data, null);
  }

  public static Payload create(ByteBuf data, @Nullable ByteBuf metadata) {
    try {
      return create(toBytes(data), metadata != null ? toBytes(metadata) : null);
    }
    finally {
      data.release();
      if (metadata != null) {
        metadata.release();
      }
    }
  }

  public static Payload create(Payload payload) {
    return create(
            toBytes(payload.data()), payload.hasMetadata() ? toBytes(payload.metadata()) : null);
  }

  private static byte[] toBytes(ByteBuf byteBuf) {
    byte[] bytes = new byte[byteBuf.readableBytes()];
    byteBuf.markReaderIndex();
    byteBuf.readBytes(bytes);
    byteBuf.resetReaderIndex();
    return bytes;
  }

  @Override
  public boolean hasMetadata() {
    return metadata != null;
  }

  @Override
  public ByteBuf sliceMetadata() {
    return metadata == null ? Unpooled.EMPTY_BUFFER : Unpooled.wrappedBuffer(metadata);
  }

  @Override
  public ByteBuf sliceData() {
    return Unpooled.wrappedBuffer(data);
  }

  @Override
  public ByteBuffer getMetadata() {
    return metadata == null ? DefaultPayload.EMPTY_BUFFER : metadata.duplicate();
  }

  @Override
  public ByteBuffer getData() {
    return data.duplicate();
  }

  @Override
  public ByteBuf data() {
    return sliceData();
  }

  @Override
  public ByteBuf metadata() {
    return sliceMetadata();
  }

  @Override
  public int refCnt() {
    return 1;
  }

  @Override
  public DefaultPayload retain() {
    return this;
  }

  @Override
  public DefaultPayload retain(int increment) {
    return this;
  }

  @Override
  public DefaultPayload touch() {
    return this;
  }

  @Override
  public DefaultPayload touch(Object hint) {
    return this;
  }

  @Override
  public boolean release() {
    return false;
  }

  @Override
  public boolean release(int decrement) {
    return false;
  }
}
