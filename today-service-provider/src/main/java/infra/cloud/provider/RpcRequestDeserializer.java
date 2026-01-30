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

package infra.cloud.provider;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import infra.cloud.RpcRequest;
import infra.cloud.serialize.Readable;
import infra.cloud.serialize.MessagePackReader;
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

  public RemoteRequest deserialize(ByteBuf payload) throws SerializationException {
    MessagePackReader input = new MessagePackReader(payload);
    RpcRequest request = new RpcRequest();
    request.readFrom(input);

    String serviceClass = input.readString();
    String methodName = input.readString();
    String[] paramTypes = input.read(String.class, Readable::readString);

    var serviceInterface = localServiceHolder.getServiceInterface(serviceClass);
    Assert.state(serviceInterface != null, "service interface not found");
    InvocableMethod method = methodMapCache.get(new MethodKey(serviceClass, methodName, paramTypes), serviceInterface);

    MethodParameter[] parameters = method.getParameters();
    Object[] args = new Object[parameters.length];

    int idx = 0;
    for (MethodParameter parameter : parameters) {
      var serialization = findArgumentSerialization(parameter);
      args[idx++] = serialization.deserialize(parameter, input);
    }

    return new RemoteRequest(method, args, serviceInterface);
  }

  private RpcArgumentSerialization findArgumentSerialization(MethodParameter parameter) {
    for (var argumentSerialization : argumentSerializations) {
      if (argumentSerialization.supportsArgument(parameter)) {
        return argumentSerialization;
      }
    }
    throw new IllegalStateException("RpcArgumentSerialization for parameter %s not found".formatted(parameter));
  }

  private final class MethodMapCache extends MapCache<MethodKey, InvocableMethod, ServiceObject> {

    @Override
    protected InvocableMethod createValue(MethodKey key, ServiceObject serviceObject) {
      Method methodToUse = getMethod(key, serviceObject.getInterface());
      if (methodToUse == null) {
        throw new IllegalStateException("No method found for method: " + key.method);
      }
      MethodInvoker methodInvoker = MethodInvoker.forMethod(methodToUse);
      ServiceInterfaceMetadata metadata = metadataProvider.getMetadata(serviceObject.getInterface());
      return new InvocableMethod(metadata, serviceObject, methodToUse, methodInvoker);
    }

    @Nullable
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
