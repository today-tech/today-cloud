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

package infra.cloud.serialize.value;

import java.util.function.BiConsumer;
import java.util.function.Function;

import infra.cloud.serialize.Readable;
import infra.cloud.serialize.SerializationException;
import infra.cloud.serialize.Writable;
import infra.core.MethodParameter;
import infra.lang.Assert;

/**
 * Defines a strategy for serializing and deserializing values of type {@code T}.
 * <p>
 * This interface provides methods to convert a Java object into a writable format
 * and to reconstruct an object from a readable format, typically used in RPC or
 * data persistence scenarios. It also offers a static factory method to create
 * instances based on functional definitions.
 *
 * @param <T> the type of value to be serialized and deserialized
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/3/8 21:31
 */
public interface ValueSerialization<T> {

  /**
   * Serializes the given value into the provided writable output.
   *
   * @param parameter the method parameter context associated with this serialization
   * @param value the value to serialize
   * @param writable the target output to write the serialized data
   * @throws SerializationException if an error occurs during serialization
   */
  void serialize(MethodParameter parameter, T value, Writable writable)
          throws SerializationException;

  /**
   * Deserializes a value from the provided readable input.
   *
   * @param parameter the method parameter context associated with this deserialization
   * @param readable the source input to read the serialized data
   * @return the deserialized value of type {@code T}
   * @throws SerializationException if an error occurs during deserialization
   */
  T deserialize(MethodParameter parameter, Readable readable)
          throws SerializationException;

  /**
   * Creates a {@code ValueSerialization} instance using the provided reader and writer functions.
   *
   * @param <T> the type of value to handle
   * @param reader a function to read and construct an object from {@link Readable}
   * @param writer a consumer to write an object to {@link Writable}
   * @return a new {@code ValueSerialization} instance
   * @throws IllegalArgumentException if either reader or writer is null
   */
  static <T> ValueSerialization<T> map(Function<Readable, T> reader, BiConsumer<Writable, T> writer) {
    Assert.notNull(reader, "reader Function is required");
    Assert.notNull(writer, "writer BiConsumer is required");
    return new FuncValueSerialization<>(reader, writer);
  }

}

