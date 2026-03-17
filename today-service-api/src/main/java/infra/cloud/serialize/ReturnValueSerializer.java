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

import infra.cloud.service.ServiceMethod;

/**
 * A serializer responsible for converting the return value of a service method into a writable format.
 * <p>
 * Implementations of this interface determine whether they support serializing the return value
 * for a given {@link ServiceMethod} and perform the actual serialization process.
 *
 * @param <T> the type of the return value to be serialized
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/20 21:56
 */
public interface ReturnValueSerializer<T> {

  /**
   * Checks if this serializer supports serializing the return value for the specified service method.
   *
   * @param method the service method to check
   * @return {@code true} if this serializer can handle the return value of the given method, {@code false} otherwise
   */
  boolean supportsReturnValue(ServiceMethod method);

  /**
   * Serializes the return value of a service method into the provided writable target.
   *
   * @param method the service method whose return value is being serialized
   * @param returnValue the return value to serialize
   * @param writable the target to write the serialized data to
   * @throws SerializationException if an error occurs during serialization
   */
  void serialize(ServiceMethod method, T returnValue, Writable writable)
          throws SerializationException;

}
