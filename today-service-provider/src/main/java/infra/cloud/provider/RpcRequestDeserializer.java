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
import infra.cloud.service.ServiceInterfaceMetadata;
import infra.cloud.service.ServiceInterfaceMetadataProvider;
import infra.core.MethodParameter;
import infra.lang.Assert;
import infra.reflect.MethodInvoker;
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

  private final ServiceInterfaceMetadataProvider metadataProvider;

  private final LocalServiceHolder localServiceHolder;

  public RpcRequestDeserializer(List<RpcArgumentSerialization> argumentSerializations,
          ServiceInterfaceMetadataProvider metadataProvider, LocalServiceHolder localServiceHolder) {
    this.argumentSerializations = argumentSerializations;
    this.metadataProvider = metadataProvider;
    this.localServiceHolder = localServiceHolder;
  }

  public RpcRequest deserialize(ByteBuf payload) throws SerializationException {
    MessagePackInput input = new MessagePackInput(payload);
    RpcRequest request = new RpcRequest();
    request.readFrom(input);

    String serviceClass = request.getServiceClass();
    Class<?> serviceInterface = localServiceHolder.getServiceInterface(serviceClass);
    Assert.state(serviceInterface != null, "service interface not found");
    InvocableMethod method = methodMapCache.get(new MethodKey(serviceClass, request.getMethodName(), request.getParamTypes()), serviceInterface);

    request.setMethod(method);

    MethodParameter[] parameters = method.getParameters();
    Object[] args = new Object[parameters.length];

    int idx = 0;
    for (MethodParameter parameter : parameters) {
      var serialization = findArgumentSerialization(parameter);
      args[idx++] = serialization.deserialize(parameter, input);
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

  private final class MethodMapCache extends MapCache<MethodKey, InvocableMethod, Class<?>> {

    @Override
    protected InvocableMethod createValue(MethodKey key, Class<?> serviceInterface) {
      Method methodToUse = getMethod(key, serviceInterface);
      if (methodToUse == null) {
        throw new IllegalStateException("No method found for method: " + key.method);
      }
      MethodInvoker methodInvoker = MethodInvoker.forMethod(methodToUse);
      ServiceInterfaceMetadata metadata = metadataProvider.getMetadata(serviceInterface);
      return new InvocableMethod(metadata, serviceInterface, methodToUse, methodInvoker);
    }

    private static Method getMethod(MethodKey key, Class<?> serviceInterface) {
      String method = key.method;
      String[] paramTypes = key.paramTypes;
      int parameterLength = paramTypes.length;

      for (Method serviceMethod : serviceInterface.getMethods()) {
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

  private record MethodKey(String serviceClass, String method, String[] paramTypes) {

  }

}
