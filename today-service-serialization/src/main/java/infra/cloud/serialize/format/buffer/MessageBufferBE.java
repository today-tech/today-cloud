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

import java.nio.ByteBuffer;

import infra.lang.Assert;

/**
 * MessageBufferBE is a {@link MessageBuffer} implementation tailored to big-endian machines.
 * The specification of Message Pack demands writing short/int/float/long/double values in the big-endian format.
 * In the big-endian machine, we do not need to swap the byte order.
 */
public class MessageBufferBE extends MessageBuffer {

  MessageBufferBE(byte[] arr, int offset, int length) {
    super(arr, offset, length);
  }

  MessageBufferBE(ByteBuffer bb) {
    super(bb);
  }

  private MessageBufferBE(Object base, long address, int length) {
    super(base, address, length);
  }

  @Override
  public MessageBufferBE slice(int offset, int length) {
    if (offset == 0 && length == size()) {
      return this;
    }
    else {
      Assert.isTrue(offset + length <= size(), "offset must <= size - length");
      return new MessageBufferBE(base, address + offset, length);
    }
  }

  @Override
  public short getShort(int index) {
    return unsafe.getShort(base, address + index);
  }

  @Override
  public int getInt(int index) {
    // We can simply return the integer value as big-endian value
    return unsafe.getInt(base, address + index);
  }

  public long getLong(int index) {
    return unsafe.getLong(base, address + index);
  }

  @Override
  public float getFloat(int index) {
    return unsafe.getFloat(base, address + index);
  }

  @Override
  public double getDouble(int index) {
    return unsafe.getDouble(base, address + index);
  }

  @Override
  public void putShort(int index, short v) {
    unsafe.putShort(base, address + index, v);
  }

  @Override
  public void putInt(int index, int v) {
    unsafe.putInt(base, address + index, v);
  }

  @Override
  public void putLong(int index, long v) {
    unsafe.putLong(base, address + index, v);
  }

  @Override
  public void putDouble(int index, double v) {
    unsafe.putDouble(base, address + index, v);
  }
}
