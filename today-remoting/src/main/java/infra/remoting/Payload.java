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

package infra.remoting;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;
import io.netty.util.ResourceLeakDetector;

/**
 * Payload of a Frame.
 */
public interface Payload extends ReferenceCounted {

  /**
   * Returns whether the payload has metadata, useful for tell if metadata is empty or not present.
   *
   * @return whether payload has non-null (possibly empty) metadata
   */
  boolean hasMetadata();

  /**
   * Returns a slice Payload metadata. Always non-null, check {@link #hasMetadata()} to
   * differentiate null from "".
   *
   * @return payload metadata.
   */
  ByteBuf sliceMetadata();

  /**
   * Returns the Payload data. Always non-null.
   *
   * @return payload data.
   */
  ByteBuf sliceData();

  /**
   * Returns the Payloads' data without slicing if possible. This is not safe and editing this could
   * effect the payload. It is recommended to call sliceData().
   *
   * @return data as a bytebuf or slice of the data
   */
  ByteBuf data();

  /**
   * Returns the Payloads' metadata without slicing if possible. This is not safe and editing this
   * could effect the payload. It is recommended to call sliceMetadata().
   *
   * @return metadata as a bytebuf or slice of the metadata
   */
  ByteBuf metadata();

  /** Increases the reference count by {@code 1}. */
  @Override
  Payload retain();

  /** Increases the reference count by the specified {@code increment}. */
  @Override
  Payload retain(int increment);

  /**
   * Records the current access location of this object for debugging purposes. If this object is
   * determined to be leaked, the information recorded by this operation will be provided to you via
   * {@link ResourceLeakDetector}. This method is a shortcut to {@link #touch(Object) touch(null)}.
   */
  @Override
  Payload touch();

  /**
   * Records the current access location of this object with an additional arbitrary information for
   * debugging purposes. If this object is determined to be leaked, the information recorded by this
   * operation will be provided to you via {@link ResourceLeakDetector}.
   */
  @Override
  Payload touch(Object hint);

  /**
   * Readable bytes
   */
  default int length() {
    return metadata().readableBytes() + data().readableBytes();
  }

  default ByteBuffer getMetadata() {
    return sliceMetadata().nioBuffer();
  }

  default ByteBuffer getData() {
    return sliceData().nioBuffer();
  }

  default String getMetadataUtf8() {
    return sliceMetadata().toString(StandardCharsets.UTF_8);
  }

  default String getDataUtf8() {
    return sliceData().toString(StandardCharsets.UTF_8);
  }
}
