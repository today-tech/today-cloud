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

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import java.lang.reflect.Method;

import infra.cloud.RpcMethod;
import infra.core.MethodParameter;
import infra.lang.Nullable;
import infra.util.ConcurrentReferenceHashMap;
import io.netty.buffer.ByteBuf;

/**
 * Serialization for protobuf
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/12/20 17:46
 */
public class ProtobufArgumentSerialization implements RpcArgumentSerialization<Message>, ReturnValueSerialization<Message> {

  private static final ConcurrentReferenceHashMap<Class<?>, Method> methodCache = new ConcurrentReferenceHashMap<>();

  @Override
  public boolean supportsArgument(MethodParameter parameter) {
    return Message.class.isAssignableFrom(parameter.getParameterType());
  }

  @Override
  public void serialize(MethodParameter parameter, @Nullable Message value, ByteBuf payload, Output output) throws SerializationException {
    output.write(value == null ? null : value.toByteArray());
  }

  @Override
  public Message deserialize(MethodParameter parameter, ByteBuf payload, Input input) throws SerializationException {
    Class<?> parameterType = parameter.getParameterType();
    Message.Builder messageBuilder = getMessageBuilder(parameterType);
    try {
      byte[] bytes = input.read();
      return messageBuilder.mergeFrom(bytes).build();
    }
    catch (InvalidProtocolBufferException e) {
      throw new SerializationException("Invalid protocol buffer", e);
    }
  }

  /**
   * Create a new {@code Message.Builder} instance for the given class.
   * <p>This method uses a ConcurrentReferenceHashMap for caching method lookups.
   */
  private Message.Builder getMessageBuilder(Class<?> clazz) {
    try {
      Method method = methodCache.get(clazz);
      if (method == null) {
        method = clazz.getMethod("newBuilder");
        methodCache.put(clazz, method);
      }
      return (Message.Builder) method.invoke(clazz);
    }
    catch (Exception ex) {
      throw new SerializationException(
              "Invalid Protobuf Message type: no invocable newBuilder() method on " + clazz, ex);
    }
  }

  // ----------------------------------------------------------------------------------------
  // ReturnValueSerialization<Message>
  // ----------------------------------------------------------------------------------------

  @Override
  public boolean supportsArgument(RpcMethod method) {
    return Message.class.isAssignableFrom(method.getReturnType().getParameterType());
  }

  @Override
  public void serialize(RpcMethod method, Message value, ByteBuf payload, Output output) {
    output.write(value.toByteArray());
  }

  @Override
  public Message deserialize(RpcMethod method, ByteBuf payload, Input input) throws SerializationException {
    return deserialize(method.getReturnType(), payload, input);
  }

}
