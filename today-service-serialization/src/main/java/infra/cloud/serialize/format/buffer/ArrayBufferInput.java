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

package infra.cloud.serialize.format.buffer;

import java.util.Objects;

/**
 * MessageBufferInput adapter for byte arrays
 */
public class ArrayBufferInput implements MessageBufferInput {

  private MessageBuffer buffer;

  private boolean isEmpty;

  public ArrayBufferInput(MessageBuffer buf) {
    this.buffer = buf;
    this.isEmpty = buf == null;
  }

  public ArrayBufferInput(byte[] arr) {
    this(arr, 0, arr.length);
  }

  public ArrayBufferInput(byte[] arr, int offset, int length) {
    this(MessageBuffer.wrap(Objects.requireNonNull(arr, "input array is null"), offset, length));
  }

  /**
   * Reset buffer. This method returns the old buffer.
   *
   * @param buf new buffer. This can be null to make this input empty.
   * @return the old buffer.
   */
  public MessageBuffer reset(MessageBuffer buf) {
    MessageBuffer old = this.buffer;
    this.buffer = buf;
    this.isEmpty = buf == null;
    return old;
  }

  public void reset(byte[] arr) {
    reset(MessageBuffer.wrap(Objects.requireNonNull(arr, "input array is null")));
  }

  public void reset(byte[] arr, int offset, int len) {
    reset(MessageBuffer.wrap(Objects.requireNonNull(arr, "input array is null"), offset, len));
  }

  @Override
  public MessageBuffer next() {
    if (isEmpty) {
      return null;
    }
    isEmpty = true;
    return buffer;
  }

  @Override
  public void close() {
    buffer = null;
    isEmpty = true;
  }
}
