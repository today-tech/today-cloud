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

package infra.cloud.serialize.format.value;

import java.nio.ByteBuffer;

import infra.cloud.serialize.format.MessageStringCodingException;

/**
 * Base interface of {@link StringValue} and {@link BinaryValue} interfaces.
 * <p/>
 * MessagePack's Raw type can represent a byte array at most 2<sup>64</sup>-1 bytes.
 *
 * @see StringValue
 * @see BinaryValue
 */
public interface RawValue
        extends Value {
  /**
   * Returns the value as {@code byte[]}.
   *
   * This method copies the byte array.
   */
  byte[] asByteArray();

  /**
   * Returns the value as {@code ByteBuffer}.
   *
   * Returned ByteBuffer is read-only. See also {@link ByteBuffer#asReadOnlyBuffer()}.
   * This method doesn't copy the byte array as much as possible.
   */
  ByteBuffer asByteBuffer();

  /**
   * Returns the value as {@code String}.
   *
   * This method throws an exception if the value includes invalid UTF-8 byte sequence.
   *
   * @throws MessageStringCodingException If this value includes invalid UTF-8 byte sequence.
   */
  String asString();

  /**
   * Returns the value as {@code String}.
   *
   * This method replaces an invalid UTF-8 byte sequence with <code>U+FFFD replacement character</code>.
   */
  String toString();
}
