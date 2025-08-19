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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import infra.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see java.io.DataOutput
 * @since 1.0 2025/8/16 16:58
 */
public interface Output {

  /**
   * Writes a byte array object to the output.
   *
   * @param b the data to add
   * @throws SerializationException if a serialization error occurs.
   * @see #writeFully(byte[])
   */
  void write(@Nullable byte[] b);

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
   * @param s the string value to be written.
   * @throws SerializationException if a serialization error occurs.
   */
  void write(@Nullable String s);

  /**
   * Writes a Timestamp value.
   *
   * @param instant the timestamp to be written
   * @throws SerializationException if a serialization error occurs.
   */
  void write(Instant instant);

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
   * @param message the Message value to be written.
   * @throws SerializationException if a serialization error occurs.
   */
  void write(Message message);

  /**
   * Writes a {@code array} value.
   *
   * @param array the array value to be written.
   * @throws SerializationException if a serialization error occurs.
   */
  <T> void write(T[] array, Consumer<T> mapper);

  /**
   * Writes a {@code array} value.
   *
   * @param array the array value to be written.
   * @throws SerializationException if a serialization error occurs.
   */
  <T> void write(T[] array, BiConsumer<Output, T> mapper);

  /**
   * Writes a {@code List} value.
   *
   * @param list the List value to be written.
   * @throws SerializationException if a serialization error occurs.
   */
  <T> void write(List<T> list, Consumer<T> mapper);

  /**
   * Writes a {@code List} value.
   *
   * @param list the List value to be written.
   * @throws SerializationException if a serialization error occurs.
   */
  <T> void write(List<T> list, BiConsumer<Output, T> mapper);

  /**
   * Writes a {@code Map} value.
   *
   * @param map the Map value to be written.
   * @throws SerializationException if a serialization error occurs.
   */
  <K, V> void write(Map<K, V> map, Consumer<K> keyMapper, Consumer<V> valueMapper);

  /**
   * Writes a {@code Map} value.
   *
   * @param map the Map value to be written.
   * @throws SerializationException if a serialization error occurs.
   */
  <K, V> void write(Map<K, V> map, BiConsumer<Output, K> keyMapper, BiConsumer<Output, V> valueMapper);

}
