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

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import infra.cloud.service.ServiceInterfaceMetadataProvider;
import infra.cloud.service.ServiceMethod;
import infra.core.ReactiveAdapterRegistry;
import infra.reflect.MethodInvoker;
import infra.util.Assert;

/**
 * Caches exported methods by service instance and immutable method signature.
 * Invalid signatures are not cached. Only metadata-exported operations are exposed.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2026/10/5
 */
public class DefaultServiceMethodResolver implements ServiceMethodResolver {

  private final ServiceInterfaceMetadataProvider<? extends ServiceMethod> metadataProvider;

  private final ReactiveAdapterRegistry adapterRegistry;

  private final LocalServiceHolder localServiceHolder;

  private final ReturnValueHandlerComposite returnValueHandlers;

  private final ConcurrentHashMap<MethodKey, InvocableMethod> methods = new ConcurrentHashMap<>();

  public DefaultServiceMethodResolver(ServiceInterfaceMetadataProvider<? extends ServiceMethod> metadataProvider,
          ReactiveAdapterRegistry adapterRegistry, LocalServiceHolder localServiceHolder) {
    this(metadataProvider, adapterRegistry, localServiceHolder, new ReturnValueHandlerComposite(List.of()));
  }

  public DefaultServiceMethodResolver(ServiceInterfaceMetadataProvider<? extends ServiceMethod> metadataProvider,
          ReactiveAdapterRegistry adapterRegistry, LocalServiceHolder localServiceHolder,
          ReturnValueHandlerComposite returnValueHandlers) {
    Assert.notNull(metadataProvider, "metadataProvider is required");
    Assert.notNull(adapterRegistry, "adapterRegistry is required");
    Assert.notNull(localServiceHolder, "localServiceHolder is required");
    this.metadataProvider = metadataProvider;
    this.adapterRegistry = adapterRegistry;
    this.localServiceHolder = localServiceHolder;
    Assert.notNull(returnValueHandlers, "returnValueHandlers is required");
    this.returnValueHandlers = returnValueHandlers;
  }

  @Override
  public InvocableMethod resolve(String serviceClass, String methodName, String[] parameterTypes) {
    Assert.notNull(serviceClass, "serviceClass is required");
    Assert.notNull(methodName, "methodName is required");
    Assert.notNull(parameterTypes, "parameterTypes is required");
    ServiceObject service = localServiceHolder.getServiceObject(serviceClass);
    Assert.state(service != null, "Service interface not found: " + serviceClass);
    var key = new MethodKey(service, methodName, List.of(parameterTypes));
    return methods.computeIfAbsent(key, this::createMethod);
  }

  private InvocableMethod createMethod(MethodKey key) {
    var metadata = metadataProvider.getMetadata(key.service.getInterface());
    for (ServiceMethod candidate : metadata.getServiceMethods()) {
      Method method = candidate.getMethod();
      if (method.getName().equals(key.name) && method.getParameterCount() == key.parameterTypes.size()) {
        Class<?>[] types = method.getParameterTypes();
        boolean matches = true;
        for (int i = 0; i < types.length; i++) {
          if (!types[i].getName().equals(key.parameterTypes.get(i))) {
            matches = false;
            break;
          }
        }
        if (matches) {
          return new InvocableMethod(metadata, key.service, method, MethodInvoker.forMethod(method), adapterRegistry,
                  returnValueHandlers);
        }
      }
    }
    throw new IllegalStateException("No exported method: " + key.service.getInterface().getName()
            + "." + key.name + key.parameterTypes);
  }

  private record MethodKey(ServiceObject service, String name, List<String> parameterTypes) {
  }

}
