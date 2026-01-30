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
import infra.cloud.serialize.Writable;
import infra.cloud.serialize.SerializationException;
import infra.core.MethodParameter;
import infra.lang.Assert;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/3/8 21:31
 */
public interface ValueSerialization<T> {

  void serialize(MethodParameter parameter, T value, Writable payload)
          throws SerializationException;

  T deserialize(MethodParameter parameter, Readable payload)
          throws SerializationException;

  static <T> ValueSerialization<T> map(Function<Readable, T> reader, BiConsumer<Writable, T> writer) {
    Assert.notNull(reader, "reader Function is required");
    Assert.notNull(writer, "writer BiConsumer is required");
    return new FuncValueSerialization<>(reader, writer);
  }

}

