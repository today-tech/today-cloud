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

import infra.lang.Assert;

/**
 * Header of the Extension types
 */
public class ExtensionTypeHeader {
  private final byte type;
  private final int length;

  /**
   * Create an extension type header
   * Example:
   * <pre>
   * {@code
   * ...
   * ExtensionTypeHeader header = new ExtensionTypeHeader(checkedCastToByte(0x01), 32);
   * ...
   * }
   * </pre>
   *
   * @param type extension type (byte). You can check the valid byte range with {@link #checkedCastToByte(int)} method.
   * @param length extension type data length
   */
  public ExtensionTypeHeader(byte type, int length) {
    Assert.isTrue(length >= 0, "length must be >= 0");
    this.type = type;
    this.length = length;
  }

  public static byte checkedCastToByte(int code) {
    Assert.isTrue(Byte.MIN_VALUE <= code && code <= Byte.MAX_VALUE, "Extension type code must be within the range of byte");
    return (byte) code;
  }

  public byte getType() {
    return type;
  }

  public boolean isTimestampType() {
    return type == MessagePack.Code.EXT_TIMESTAMP;
  }

  public int getLength() {
    return length;
  }

  @Override
  public int hashCode() {
    return (type + 31) * 31 + length;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ExtensionTypeHeader other) {
      return this.type == other.type && this.length == other.length;
    }
    return false;
  }

  @Override
  public String toString() {
    return String.format("ExtensionTypeHeader(type:%d, length:%,d)", type, length);
  }
}
