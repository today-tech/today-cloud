/*
 * Copyright 2021 - 2026 the original author or authors.
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

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import infra.beans.BeanUtils;
import infra.cloud.serialize.value.ValueSerialization;
import infra.core.MethodParameter;

import static infra.cloud.serialize.value.ValueSerialization.map;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/3/8 21:15
 */
public class SimpleValueArgumentSerialization implements RpcArgumentSerialization<Object> {

  private final Map<Class<?>, ValueSerialization<?>> serializationMap = new HashMap<>();

  public SimpleValueArgumentSerialization() {
    serializationMap.put(int.class, map(Readable::readInt, Writable::write));
    serializationMap.put(Integer.class, map(Readable::readInt, Writable::write));

    serializationMap.put(long.class, map(Readable::readLong, Writable::write));
    serializationMap.put(Long.class, map(Readable::readLong, Writable::write));

    serializationMap.put(short.class, map(Readable::readShort, Writable::write));
    serializationMap.put(Short.class, map(Readable::readShort, Writable::write));
  }

  @Override
  public boolean supportsArgument(MethodParameter parameter) {
    return BeanUtils.isSimpleProperty(parameter.getParameterType());
  }

  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void serialize(MethodParameter parameter, @Nullable Object value, Writable writable) {
    ValueSerialization serialization = findSerialization(parameter.getParameterType());
    serialization.serialize(parameter, value, writable);
  }

  @Nullable
  @Override
  public Object deserialize(MethodParameter parameter, Readable readable) {
    var serialization = findSerialization(parameter.getParameterType());
    return serialization.deserialize(parameter, readable);
  }

  @SuppressWarnings({ "rawtypes" })
  private ValueSerialization findSerialization(Class<?> type) {
    ValueSerialization<?> serialization = serializationMap.get(type);
    if (serialization == null) {
      Class<?> superclass = type.getSuperclass();
      if (superclass == null || superclass == Object.class) {
        throw new IllegalStateException("ValueSerialization for type %s not found".formatted(type)); // todo type
      }
      return findSerialization(superclass);
    }
    return serialization;
  }

}
