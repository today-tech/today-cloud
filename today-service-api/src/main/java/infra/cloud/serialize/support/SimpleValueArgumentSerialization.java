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

package infra.cloud.serialize.support;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import infra.cloud.serialize.ArgumentSerialization;
import infra.cloud.serialize.Readable;
import infra.cloud.serialize.Writable;
import infra.cloud.serialize.value.ValueSerialization;
import infra.core.MethodParameter;

import static infra.cloud.serialize.value.ValueSerialization.map;

/**
 * A simple implementation of {@link ArgumentSerialization} that handles basic value types.
 * <p>
 * This class provides serialization and deserialization support for primitive types
 * and their corresponding wrapper classes, including {@code int}, {@code long}, and {@code short}.
 * It maintains an internal map to associate each supported type with its specific
 * {@link ValueSerialization} logic.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/3/8 21:15
 */
public class SimpleValueArgumentSerialization implements ArgumentSerialization<Object> {

  private final Map<Class<?>, ValueSerialization<?>> serializationMap = new HashMap<>();

  public SimpleValueArgumentSerialization() {
    serializationMap.put(int.class, map(infra.cloud.serialize.Readable::readInt, Writable::write));
    serializationMap.put(Integer.class, map(infra.cloud.serialize.Readable::readInt, Writable::write));

    serializationMap.put(long.class, map(infra.cloud.serialize.Readable::readLong, Writable::write));
    serializationMap.put(Long.class, map(infra.cloud.serialize.Readable::readLong, Writable::write));

    serializationMap.put(short.class, map(infra.cloud.serialize.Readable::readShort, Writable::write));
    serializationMap.put(Short.class, map(infra.cloud.serialize.Readable::readShort, Writable::write));
  }

  @Override
  public boolean supportsArgument(MethodParameter parameter) {
    return serializationMap.containsKey(parameter.getParameterType());
  }

  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void serialize(MethodParameter parameter, @Nullable Object value, Writable writable) {
    ValueSerialization serialization = findSerialization(parameter.getParameterType());
    serialization.serialize(parameter, value, writable);
  }

  @Override
  public @Nullable Object deserialize(MethodParameter parameter, Readable readable) {
    var serialization = findSerialization(parameter.getParameterType());
    return serialization.deserialize(parameter, readable);
  }

  @SuppressWarnings({ "rawtypes" })
  private ValueSerialization findSerialization(Class<?> type) {
    ValueSerialization<?> serialization = serializationMap.get(type);
    if (serialization == null) {
      Class<?> superclass = type.getSuperclass();
      if (superclass == null || superclass == Object.class) {
        throw new IllegalStateException("ValueSerialization for type %s not found".formatted(type));
      }
      return findSerialization(superclass);
    }
    return serialization;
  }

}
