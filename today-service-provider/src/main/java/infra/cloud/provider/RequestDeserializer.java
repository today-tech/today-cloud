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

import java.util.List;

import infra.cloud.serialize.ArgumentSerialization;
import infra.cloud.serialize.Readable;
import infra.cloud.serialize.SerializationException;
import infra.cloud.service.ServiceInterfaceMetadataProvider;
import infra.core.MethodParameter;
import infra.core.ReactiveAdapterRegistry;
import infra.util.Assert;

/**
 * Deserializes incoming remote requests
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/3/8 22:43
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class RequestDeserializer {

  private final List<ArgumentSerialization> argumentSerializations;

  private final ServiceMethodResolver methodResolver;

  public RequestDeserializer(List<ArgumentSerialization> argumentSerializations,
          ServiceInterfaceMetadataProvider metadataProvider, LocalServiceHolder localServiceHolder) {
    this(argumentSerializations, metadataProvider, localServiceHolder, ReactiveAdapterRegistry.getSharedInstance());
  }

  public RequestDeserializer(List<ArgumentSerialization> argumentSerializations,
          ServiceInterfaceMetadataProvider metadataProvider, LocalServiceHolder localServiceHolder,
          ReactiveAdapterRegistry adapterRegistry) {
    this(argumentSerializations, new DefaultServiceMethodResolver(metadataProvider, adapterRegistry, localServiceHolder));
  }

  public RequestDeserializer(List<ArgumentSerialization> argumentSerializations,
          ServiceMethodResolver methodResolver) {
    Assert.notNull(methodResolver, "methodResolver is required");
    this.argumentSerializations = List.copyOf(argumentSerializations);
    this.methodResolver = methodResolver;
  }

  public RemoteRequest deserialize(Readable readable) throws SerializationException {
    String serviceClass = readable.readString();
    String methodName = readable.readString();
    String[] paramTypes = readable.read(String.class, Readable::readString);

    InvocableMethod method = methodResolver.resolve(serviceClass, methodName, paramTypes);

    MethodParameter[] parameters = method.getParameters();
    @Nullable Object[] args = new Object[parameters.length];

    int idx = 0;
    for (MethodParameter parameter : parameters) {
      var serialization = findArgumentSerialization(parameter);
      args[idx++] = serialization.deserialize(parameter, readable);
    }

    return new RemoteRequest(method, args, method.getServiceObject());
  }

  private ArgumentSerialization findArgumentSerialization(MethodParameter parameter) {
    for (var argumentSerialization : argumentSerializations) {
      if (argumentSerialization.supportsArgument(parameter)) {
        return argumentSerialization;
      }
    }
    throw new IllegalStateException("ArgumentSerialization for parameter %s not found".formatted(parameter));
  }

}
