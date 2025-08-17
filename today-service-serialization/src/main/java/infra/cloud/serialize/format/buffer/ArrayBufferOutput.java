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

import java.util.ArrayList;
import java.util.List;

/**
 * MessageBufferOutput adapter that writes data into a list of byte arrays.
 * <p>
 * This class allocates a new buffer instead of resizing the buffer when data doesn't fit in the initial capacity.
 * This is faster than ByteArrayOutputStream especially when size of written bytes is large because resizing a buffer
 * usually needs to copy contents of the buffer.
 */
public class ArrayBufferOutput implements MessageBufferOutput {

  private final ArrayList<MessageBuffer> list;

  private final int bufferSize;

  private MessageBuffer lastBuffer;

  public ArrayBufferOutput() {
    this(8192);
  }

  public ArrayBufferOutput(int bufferSize) {
    this.bufferSize = bufferSize;
    this.list = new ArrayList<>();
  }

  /**
   * Gets the size of the written data.
   *
   * @return number of bytes
   */
  public int getSize() {
    int size = 0;
    for (MessageBuffer buffer : list) {
      size += buffer.size();
    }
    return size;
  }

  /**
   * Gets a copy of the written data as a byte array.
   * <p>
   * If your application needs better performance and smaller memory consumption, you may prefer
   * {@link #toMessageBuffer()} or {@link #toBufferList()} to avoid copying.
   *
   * @return the byte array
   */
  public byte[] toByteArray() {
    byte[] data = new byte[getSize()];
    int off = 0;
    for (MessageBuffer buffer : list) {
      buffer.getBytes(0, data, off, buffer.size());
      off += buffer.size();
    }
    return data;
  }

  /**
   * Gets the written data as a MessageBuffer.
   * <p>
   * Unlike {@link #toByteArray()}, this method omits copy of the contents if size of the written data is smaller
   * than a single buffer capacity.
   *
   * @return the MessageBuffer instance
   */
  public MessageBuffer toMessageBuffer() {
    if (list.size() == 1) {
      return list.get(0);
    }
    else if (list.isEmpty()) {
      return MessageBuffer.allocate(0);
    }
    else {
      return MessageBuffer.wrap(toByteArray());
    }
  }

  /**
   * Returns the written data as a list of MessageBuffer.
   * <p>
   * Unlike {@link #toByteArray()} or {@link #toMessageBuffer()}, this is the fastest method that doesn't
   * copy contents in any cases.
   *
   * @return the list of MessageBuffer instances
   */
  public List<MessageBuffer> toBufferList() {
    return new ArrayList<>(list);
  }

  /**
   * Clears the written data.
   */
  public void clear() {
    list.clear();
  }

  @Override
  public MessageBuffer next(int minimumSize) {
    if (lastBuffer != null && lastBuffer.size() > minimumSize) {
      return lastBuffer;
    }
    else {
      int size = Math.max(bufferSize, minimumSize);
      MessageBuffer buffer = MessageBuffer.allocate(size);
      lastBuffer = buffer;
      return buffer;
    }
  }

  @Override
  public void writeBuffer(int length) {
    list.add(lastBuffer.slice(0, length));
    if (lastBuffer.size() - length > bufferSize / 4) {
      lastBuffer = lastBuffer.slice(length, lastBuffer.size() - length);
    }
    else {
      lastBuffer = null;
    }
  }

  @Override
  public void write(byte[] buffer, int offset, int length) {
    MessageBuffer copy = MessageBuffer.allocate(length);
    copy.putBytes(0, buffer, offset, length);
    list.add(copy);
  }

  @Override
  public void add(byte[] buffer, int offset, int length) {
    MessageBuffer wrapped = MessageBuffer.wrap(buffer, offset, length);
    list.add(wrapped);
  }

  @Override
  public void close() {
  }

  @Override
  public void flush() {
  }
}
