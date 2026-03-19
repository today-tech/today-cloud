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

package infra.cloud.provider;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import infra.cloud.serialize.ArgumentSerialization;
import infra.cloud.serialize.Readable;
import infra.cloud.serialize.SerializationException;
import infra.cloud.service.ServiceInterfaceMetadata;
import infra.cloud.service.ServiceInterfaceMetadataProvider;
import infra.core.MethodParameter;
import infra.lang.Assert;
import infra.reflect.MethodInvoker;
import infra.util.MapCache;

/**
 * Deserializes incoming remote requests
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/3/8 22:43
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class RequestDeserializer {

  private final List<ArgumentSerialization> argumentSerializations;

  /** fast method mapping cache */
  private final MethodMapCache methodMapCache = new MethodMapCache();

  private final ServiceInterfaceMetadataProvider metadataProvider;

  private final LocalServiceHolder localServiceHolder;

  public RequestDeserializer(List<ArgumentSerialization> argumentSerializations,
          ServiceInterfaceMetadataProvider metadataProvider, LocalServiceHolder localServiceHolder) {
    this.argumentSerializations = argumentSerializations;
    this.metadataProvider = metadataProvider;
    this.localServiceHolder = localServiceHolder;
  }

  public RemoteRequest deserialize(Readable readable) throws SerializationException {
    String serviceClass = readable.readString();
    String methodName = readable.readString();
    String[] paramTypes = readable.read(String.class, Readable::readString);

    var serviceInterface = localServiceHolder.getServiceObject(serviceClass);
    Assert.state(serviceInterface != null, "service interface not found");
    InvocableMethod method = methodMapCache.get(new MethodKey(serviceClass, methodName, paramTypes), serviceInterface);

    MethodParameter[] parameters = method.getParameters();
    @Nullable Object[] args = new Object[parameters.length];

    int idx = 0;
    for (MethodParameter parameter : parameters) {
      var serialization = findArgumentSerialization(parameter);
      args[idx++] = serialization.deserialize(parameter, readable);
    }

    return new RemoteRequest(method, args, serviceInterface);
  }

  private ArgumentSerialization findArgumentSerialization(MethodParameter parameter) {
    for (var argumentSerialization : argumentSerializations) {
      if (argumentSerialization.supportsArgument(parameter)) {
        return argumentSerialization;
      }
    }
    throw new IllegalStateException("ArgumentSerialization for parameter %s not found".formatted(parameter));
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
