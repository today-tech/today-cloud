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

package infra.cloud.provider;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import infra.cloud.RpcRequest;
import infra.cloud.serialize.MessagePackInput;
import infra.cloud.serialize.RpcArgumentSerialization;
import infra.cloud.serialize.SerializationException;
import infra.core.MethodParameter;
import infra.lang.Nullable;
import infra.reflect.MethodInvoker;
import infra.util.ClassUtils;
import infra.util.MapCache;
import io.netty.buffer.ByteBuf;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/3/8 22:43
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class RpcRequestDeserializer {

  private final List<RpcArgumentSerialization> argumentSerializations;

  /** fast method mapping cache */
  private final MethodMapCache methodMapCache = new MethodMapCache();

  public RpcRequestDeserializer(List<RpcArgumentSerialization> argumentSerializations) {
    this.argumentSerializations = argumentSerializations;
  }

  public RpcRequest deserialize(ByteBuf payload) throws SerializationException {
    MessagePackInput input = new MessagePackInput(payload);

    RpcRequest request = new RpcRequest();
    request.readFrom(input);

    InvocableRpcMethod rpcMethod = methodMapCache.get(new MethodKey(request.getMethodName(), request.getParamTypes()), null);

    if (rpcMethod == null) {
      throw new IllegalStateException("No method found for method: " + request.getMethodName());
    }

    request.setRpcMethod(rpcMethod);

    MethodParameter[] parameters = rpcMethod.getParameters();
    Object[] args = new Object[parameters.length];

    int idx = 0;
    for (MethodParameter parameter : parameters) {
      var serialization = findArgumentSerialization(parameter);
      args[idx++] = serialization.deserialize(parameter, payload, input);
    }

    request.setArguments(args);
    return request;
  }

  private RpcArgumentSerialization findArgumentSerialization(MethodParameter parameter) {
    for (var argumentSerialization : argumentSerializations) {
      if (argumentSerialization.supportsArgument(parameter)) {
        return argumentSerialization;
      }
    }
    throw new IllegalStateException("RpcArgumentSerialization for parameter %s not found".formatted(parameter));
  }

  private static final class MethodMapCache extends MapCache<MethodKey, InvocableRpcMethod, Object> {

    @Nullable
    @Override
    protected InvocableRpcMethod createValue(MethodKey key, @Nullable Object service) {
      Method methodToUse = getMethod(key, service);
      if (methodToUse == null) {
        return null;
      }
      MethodInvoker methodInvoker = MethodInvoker.forMethod(methodToUse);
      return new InvocableRpcMethod(methodToUse, methodInvoker);
    }

    private static Method getMethod(MethodKey key, Object service) {
      String method = key.method;
      String[] paramTypes = key.paramTypes;
      int parameterLength = paramTypes.length;

      Class<Object> serviceImpl = ClassUtils.getUserClass(service);
      for (Method serviceMethod : serviceImpl.getMethods()) {
        if (Objects.equals(serviceMethod.getName(), method)
                && parameterLength == serviceMethod.getParameterCount()) {
          int current = 0;
          boolean equals = true;
          for (Class<?> parameterType : serviceMethod.getParameterTypes()) {
            if (!parameterType.getName().equals(paramTypes[current++])) {
              // not target method
              equals = false;
              break;
            }
          }
          if (equals) {
            return serviceMethod;
          }
        }
      }
      return null;
    }
  }

  private record MethodKey(String method, String[] paramTypes) {

  }

}
