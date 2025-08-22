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

package infra.cloud.serialize;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import infra.lang.Nullable;

/**
 * An Input lets an application read primitive data types and objects from a source of data.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see java.io.DataInput
 * @since 1.0 2025/8/16 16:59
 */
public interface Input {

  /**
   * Reads some bytes from an input
   * stream and stores them into the buffer
   * array {@code b}. The number of bytes
   * read is equal
   * to the length of {@code b}.
   *
   * @param b the buffer into which the data is read.
   * @throws NullPointerException if {@code b} is {@code null}.
   */
  void read(byte[] b);

  /**
   * Reads {@code len} bytes from an input.
   *
   * @param b the buffer into which the data is read.
   * @param off an int specifying the offset in the data array {@code b}.
   * @param len an int specifying the number of bytes to read.
   * @throws NullPointerException if {@code b} is {@code null}.
   * @throws IndexOutOfBoundsException if {@code off} is negative,
   * {@code len} is negative, or {@code len} is greater than
   * {@code b.length - off}.
   */
  void read(byte[] b, int off, int len);

  /**
   * Reads byte array
   */
  byte[] read();

  /**
   * Reads bytes with given length
   */
  byte[] read(int len);

  /**
   * Reads all left bytes
   */
  byte[] readFully();

  /**
   * Skip bytes
   *
   * @param n the number of bytes to be skipped.
   * @return the number of bytes actually skipped.
   * @throws SerializationException if a serialization error occurs.
   */
  int skipBytes(int n);

  /**
   * Reads a {@code boolean} value.
   *
   * @return the {@code boolean} value read.
   * @throws SerializationException if a serialization error occurs.
   */
  boolean readBoolean();

  /**
   * Reads a {@code byte} value.
   *
   * @return the 8-bit value read.
   * @throws SerializationException if a serialization error occurs.
   */
  byte readByte();

  /**
   * Reads a unsigned {@code byte} value.
   *
   * @return the unsigned 8-bit value read.
   * @throws SerializationException if a serialization error occurs.
   */
  int readUnsignedByte();

  /**
   * Reads a {@code short} value.
   *
   * @return the 16-bit value read.
   * @throws SerializationException if a serialization error occurs.
   */
  short readShort();

  /**
   * Reads a unsigned {@code short} value.
   *
   * @return the unsigned 16-bit value read.
   * @throws SerializationException if a serialization error occurs.
   */
  int readUnsignedShort();

  /**
   * Reads a {@code int} value.
   *
   * @return the {@code int} value read.
   * @throws SerializationException if a serialization error occurs.
   */
  int readInt();

  /**
   * Reads a {@code long} value.
   *
   * @return the {@code long} value read.
   * @throws SerializationException if a serialization error occurs.
   */
  long readLong();

  /**
   * Reads a {@code float} value.
   *
   * @return the {@code float} value read.
   * @throws SerializationException if a serialization error occurs.
   */
  float readFloat();

  /**
   * Reads a {@code double} value.
   *
   * @return the {@code double} value read.
   * @throws SerializationException if a serialization error occurs.
   */
  double readDouble();

  /**
   * Reads a {@link String} value.
   *
   * @return a string.
   * @throws SerializationException if a serialization error occurs.
   */
  String readString();

  /**
   * Reads a {@link Instant} value.
   *
   * @return an Instant object.
   * @throws SerializationException if a serialization error occurs.
   */
  Instant readTimestamp();

  /**
   * Reads a {@link Message} value.
   *
   * @throws SerializationException if a serialization error occurs.
   */
  void read(Message message);

  @Nullable
  <V> V readNullable(Function<Input, V> valueMapper);

  /**
   * Reads a {@code array} value.
   *
   * @return a array object.
   * @throws SerializationException if a serialization error occurs.
   */
  <T> T[] read(Class<T> type, Function<Input, T> mapper);

  /**
   * Reads a {@code array} value.
   *
   * @return a array object.
   * @throws SerializationException if a serialization error occurs.
   */
  <T> T[] read(Class<T> type, Supplier<T> supplier);

  /**
   * Reads a {@link List} value.
   *
   * @return a List object.
   * @throws SerializationException if a serialization error occurs.
   */
  <T> List<T> read(Function<Input, T> mapper);

  /**
   * Reads a {@link List} value.
   *
   * @return a List object.
   * @throws SerializationException if a serialization error occurs.
   */
  <T> List<T> read(Supplier<T> supplier);

  /**
   * Reads a {@link Map} value.
   *
   * @return a Map object.
   * @throws SerializationException if a serialization error occurs.
   */
  <K, V> Map<K, V> read(Function<Input, K> keyMapper, Function<Input, V> valueMapper);

}
