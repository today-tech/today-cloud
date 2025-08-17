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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import infra.beans.BeanUtils;
import infra.cloud.serialize.value.ValueSerialization;
import infra.core.MethodParameter;
import infra.lang.Nullable;
import io.netty.buffer.ByteBuf;

import static infra.cloud.serialize.value.ValueSerialization.map;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/3/8 21:15
 */
public class SimpleValueArgumentSerialization implements RpcArgumentSerialization<Object> {

  private final Map<Class<?>, ValueSerialization<?>> serializationMap = new HashMap<>();

  public SimpleValueArgumentSerialization() {
    serializationMap.put(int.class, map(ByteBuf::readInt, ByteBuf::writeInt));
    serializationMap.put(Integer.class, map(ByteBuf::readInt, ByteBuf::writeInt));

    serializationMap.put(long.class, map(ByteBuf::readLong, ByteBuf::writeLong));
    serializationMap.put(Long.class, map(ByteBuf::readLong, ByteBuf::writeLong));

//    serializationMap.put(short.class, map(ByteBuf::readShort, ByteBuf::writeShort));
//    serializationMap.put(Short.class, map(ByteBuf::readShort, ByteBuf::writeShort));

  }

  @Override
  public boolean supportsArgument(MethodParameter parameter) {
    return BeanUtils.isSimpleProperty(parameter.getParameterType());
  }

  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void serialize(MethodParameter parameter, @Nullable Object value, ByteBuf payload, Output output) throws IOException {
    payload.writeBoolean(value != null);
    if (value != null) {
      ValueSerialization serialization = findSerialization(parameter.getParameterType());
      serialization.serialize(parameter, value, payload);
    }
  }

  @Nullable
  @Override
  public Object deserialize(MethodParameter parameter, ByteBuf payload, Input input) throws SerializationException {
    boolean isNull = payload.readBoolean();
    if (isNull) {
      return null;
    }

    var serialization = findSerialization(parameter.getParameterType());
    return serialization.deserialize(parameter, payload);
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
