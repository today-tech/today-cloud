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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import infra.lang.Enumerable;

/**
 * Interface for writing data to an output stream.
 * Provides methods for writing various data types including primitives, arrays, collections, and objects.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see java.io.DataOutput
 * @since 1.0 2025/8/16 16:58
 */
public interface Writable {

  /**
   * Writes a byte array object to the output.
   *
   * @param b the data to add
   * @throws SerializationException if a serialization error occurs.
   * @see #writeFully(byte[])
   */
  void write(byte @Nullable [] b);

  /**
   * Writes a byte array object to the output.
   *
   * @param b the data to add
   * @throws SerializationException if a serialization error occurs.
   * @see #writeFully(byte[])
   */
  void write(byte[] b, int off, int len);

  /**
   * Append byte array to this Output
   */
  void writeFully(byte[] b);

  /**
   * Append byte array to this Output
   */
  void writeFully(byte[] b, int off, int len);

  /**
   * Writes a {@code boolean} value.
   *
   * @param v the boolean to be written.
   * @throws SerializationException if a serialization error occurs.
   */
  void write(boolean v);

  /**
   * Writes a {@code byte} value.
   *
   * @param b the byte to be written.
   * @throws SerializationException if a serialization error occurs.
   */
  void write(byte b);

  /**
   * Writes a {@code short} value.
   *
   * @param v the {@code short} value to be written.
   * @throws SerializationException if a serialization error occurs.
   */
  void write(short v);

  /**
   * Writes an {@code int} value.
   *
   * @param v the {@code int} value to be written.
   * @throws SerializationException if a serialization error occurs.
   */
  void write(int v);

  /**
   * Writes a {@code long} value.
   *
   * @param v the integer to be written
   * @throws SerializationException if write failed.
   */
  void write(long v);

  /**
   * Writes a {@code float} value.
   *
   * @param v the {@code float} value to be written.
   * @throws SerializationException if a serialization error occurs.
   */
  void write(float v);

  /**
   * Writes a {@code double} value.
   *
   * @param v the {@code double} value to be written.
   * @throws SerializationException if a serialization error occurs.
   */
  void write(double v);

  /**
   * Writes a {@code String} value.
   *
   * @param v the string value to be written.
   * @throws SerializationException if a serialization error occurs.
   */
  void write(@Nullable String v);

  /**
   * Writes an {@code Enumerable<Integer>} value.
   *
   * @param v the enumerable integer value to be written.
   * @throws SerializationException if a serialization error occurs.
   */
  void write(Enumerable<Integer> v);

  /**
   * Writes a Timestamp value.
   *
   * @param v the timestamp to be written
   * @throws SerializationException if a serialization error occurs.
   */
  void write(Instant v);

  /**
   * Writes a Timestamp value.
   *
   * @param epochSecond the number of seconds from 1970-01-01T00:00:00Z
   * @param nanoAdjustment the nanosecond adjustment to the number of seconds, positive or negative
   * @throws SerializationException if a serialization error occurs.
   */
  void writeTimestamp(long epochSecond, int nanoAdjustment);

  /**
   * Writes a Timestamp value using a millisecond value (e.g., System.currentTimeMillis())
   *
   * @param millis the millisecond value
   * @throws SerializationException if a serialization error occurs.
   */
  void writeTimestamp(long millis);

  /**
   * Writes a {@code Message} value.
   *
   * @param v the Message value to be written.
   * @throws SerializationException if a serialization error occurs.
   */
  void write(Message v);

  /**
   * Writes a nullable {@code V} value.
   *
   * @param v the value to be written.
   * @throws SerializationException if a serialization error occurs.
   */
  <V> boolean writeNullable(@Nullable V v, BiConsumer<Writable, V> valueMapper);

  /**
   * Writes a {@code array} value.
   *
   * @param v the array value to be written.
   * @throws SerializationException if a serialization error occurs.
   */
  <T> void write(T[] v, Consumer<T> mapper);

  /**
   * Writes a {@code array} value.
   *
   * @param v the array value to be written.
   * @throws SerializationException if a serialization error occurs.
   */
  <T> void write(T[] v, BiConsumer<Writable, T> mapper);

  /**
   * Writes a {@code List} value.
   *
   * @param v the List value to be written.
   * @throws SerializationException if a serialization error occurs.
   */
  <T> void write(List<T> v, Consumer<T> mapper);

  /**
   * Writes a {@code List} value.
   *
   * @param v the List value to be written.
   * @throws SerializationException if a serialization error occurs.
   */
  <T> void write(List<T> v, BiConsumer<Writable, T> mapper);

  /**
   * Writes a {@code Map} value.
   *
   * @param v the Map value to be written.
   * @throws SerializationException if a serialization error occurs.
   */
  <K, V> void write(Map<K, V> v, Consumer<K> keyMapper, Consumer<V> valueMapper);

  /**
   * Writes a {@code Map} value.
   *
   * @param v the Map value to be written.
   * @throws SerializationException if a serialization error occurs.
   */
  <K, V> void write(Map<K, V> v, BiConsumer<Writable, K> keyMapper, BiConsumer<Writable, V> valueMapper);

}
