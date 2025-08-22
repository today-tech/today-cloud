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

package infra.cloud.serialize.format;

import java.io.IOException;
import java.util.List;

import infra.cloud.serialize.format.buffer.ArrayBufferOutput;
import infra.cloud.serialize.format.buffer.MessageBuffer;
import infra.cloud.serialize.format.buffer.MessageBufferOutput;

/**
 * MessagePacker that is useful to produce byte array output.
 * <p>
 * This class allocates a new buffer instead of resizing the buffer when data doesn't fit in the initial capacity.
 * This is faster than ByteArrayOutputStream especially when size of written bytes is large because resizing a buffer
 * usually needs to copy contents of the buffer.
 */
public class MessageBufferPacker extends MessagePacker {

  protected MessageBufferPacker(MessagePack.PackerConfig config) {
    this(new ArrayBufferOutput(config.getBufferSize()), config);
  }

  protected MessageBufferPacker(ArrayBufferOutput out, MessagePack.PackerConfig config) {
    super(out, config);
  }

  public MessageBufferOutput reset(MessageBufferOutput out) throws IOException {
    if (!(out instanceof ArrayBufferOutput)) {
      throw new IllegalArgumentException("MessageBufferPacker accepts only ArrayBufferOutput");
    }
    return super.reset(out);
  }

  private ArrayBufferOutput getArrayBufferOut() {
    return (ArrayBufferOutput) out;
  }

  @Override
  public void clear() {
    super.clear();
    getArrayBufferOut().clear();
  }

  /**
   * Gets copy of the written data as a byte array.
   * <p>
   * If your application needs better performance and smaller memory consumption, you may prefer
   * {@link #toMessageBuffer()} or {@link #toBufferList()} to avoid copying.
   *
   * @return the byte array
   */
  public byte[] toByteArray() {
    try {
      flush();
    }
    catch (IOException ex) {
      // IOException must not happen because underlying ArrayBufferOutput never throws IOException
      throw new RuntimeException(ex);
    }
    return getArrayBufferOut().toByteArray();
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
    try {
      flush();
    }
    catch (IOException ex) {
      // IOException must not happen because underlying ArrayBufferOutput never throws IOException
      throw new RuntimeException(ex);
    }
    return getArrayBufferOut().toMessageBuffer();
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
    try {
      flush();
    }
    catch (IOException ex) {
      // IOException must not happen because underlying ArrayBufferOutput never throws IOException
      throw new RuntimeException(ex);
    }
    return getArrayBufferOut().toBufferList();
  }

  /**
   * @return the size of the buffer in use
   */
  public int getBufferSize() {
    return getArrayBufferOut().getSize();
  }
}
