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

package infra.cloud.core.serialize;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.protostuff.ByteBufferInput;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

public abstract class ProtostuffUtils {

  private static final Map<Class<?>, Schema<?>> schemaCache = new ConcurrentHashMap<>();

  @SuppressWarnings("unchecked")
  public static <T> byte[] serialize(T obj) {
    final LinkedBuffer buffer = LinkedBuffer.allocate();

    Class<T> clazz = (Class<T>) obj.getClass();
    Schema<T> schema = getSchema(clazz);
    byte[] data;
    try {
      data = ProtostuffIOUtil.toByteArray(obj, schema, buffer);
    }
    finally {
      buffer.clear();
    }

    return data;
  }

  public static <T> T deserialize(byte[] data, Class<T> clazz) {
    Schema<T> schema = getSchema(clazz);
    T obj = schema.newMessage();
    ProtostuffIOUtil.mergeFrom(data, obj, schema);
    return obj;
  }

  public static <T> T deserialize(ByteBuffer data, Class<T> clazz) throws IOException {
    Schema<T> schema = getSchema(clazz);
    T message = schema.newMessage();
    ByteBufferInput input = new ByteBufferInput(data, true);
    schema.mergeFrom(input, message);
    return message;
  }

  @SuppressWarnings("unchecked")
  private static <T> Schema<T> getSchema(Class<T> clazz) {
    Schema<T> schema = (Schema<T>) schemaCache.get(clazz);
    if (schema == null) {
      schema = RuntimeSchema.getSchema(clazz);
      if (schema != null) {
        schemaCache.put(clazz, schema);
      }
    }

    return schema;
  }
}
