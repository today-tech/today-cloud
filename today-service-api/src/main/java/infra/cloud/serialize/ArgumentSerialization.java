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

import infra.core.MethodParameter;

/**
 * A strategy interface for serializing and deserializing method arguments.
 * <p>
 * Implementations of this interface define how specific argument types are converted
 * to a wire format (serialization) and reconstructed from it (deserialization).
 * </p>
 *
 * @param <T> the type of the argument being serialized or deserialized
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/12/20 16:29
 */
public interface ArgumentSerialization<T> {

  /**
   * Checks whether this serializer supports the given method parameter.
   * <p> This method is typically used to determine if the current implementation
   * can handle the serialization/deserialization logic for the specified parameter.
   *
   * @param parameter the method parameter to check
   * @return {@code true} if this serializer supports the parameter; {@code false} otherwise
   */
  boolean supportsArgument(MethodParameter parameter);

  /**
   * Serializes the given value based on the provided method parameter.
   * <p> The serialized data is written to the provided {@link Writable} instance.
   *
   * @param parameter the method parameter associated with the value
   * @param value the value to serialize, may be {@code null}
   * @param writable the destination to write the serialized data to
   * @throws SerializationException if an error occurs during serialization
   */
  void serialize(MethodParameter parameter, @Nullable T value, Writable writable)
          throws SerializationException;

  /**
   * Deserializes a value from the provided readable source based on the method parameter.
   * <p> Reads data from the {@link Readable} instance and reconstructs the original object.
   *
   * @param parameter the method parameter describing the expected type
   * @param readable the source to read the serialized data from
   * @return the deserialized value, or {@code null} if applicable
   * @throws SerializationException if an error occurs during deserialization
   */
  @Nullable
  T deserialize(MethodParameter parameter, Readable readable) throws SerializationException;

}
