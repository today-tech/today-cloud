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

package infra.cloud.serialize;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import infra.lang.Enumerable;

/**
 * A readable interface that allows an application to read
 * primitive data types and objects from a data source.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see java.io.DataInput
 * @since 1.0 2025/8/16 16:59
 */
public interface Readable {

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

  /**
   * Reads an enum value.
   *
   * @param type the enum class type.
   * @return an enum value.
   * @throws SerializationException if a serialization error occurs.
   */
  <V extends Enumerable<Integer>> V readEnum(Class<V> type);

  /**
   * Reads a nullable value.
   *
   * @param valueMapper function to map the value from readable.
   * @return a nullable value.
   * @throws SerializationException if a serialization error occurs.
   */
  <V extends @Nullable Object> V readNullable(Function<Readable, V> valueMapper);

  /**
   * Reads a {@code array} value.
   *
   * @return a array object.
   * @throws SerializationException if a serialization error occurs.
   */
  <T extends @Nullable Object> T[] read(Class<T> type, Function<Readable, T> mapper);

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
  <T> List<T> read(Function<Readable, T> mapper);

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
  <K, V> Map<K, V> read(Function<Readable, K> keyMapper, Function<Readable, V> valueMapper);

}
