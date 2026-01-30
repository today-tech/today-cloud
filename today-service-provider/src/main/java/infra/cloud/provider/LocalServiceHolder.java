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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import infra.beans.factory.SmartInitializingSingleton;
import infra.cloud.service.ServiceMetadata;
import infra.cloud.service.ServiceMetadataProvider;
import infra.context.ApplicationContext;
import infra.context.support.ApplicationObjectSupport;
import infra.lang.Assert;
import infra.stereotype.Service;
import infra.util.ClassUtils;
import infra.util.MultiValueMap;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/10/19 21:40
 */
public class LocalServiceHolder extends ApplicationObjectSupport implements SmartInitializingSingleton, ServicesProvider {

  private final HashMap<Class<?>, Object> localServices = new HashMap<>();

  private final HashMap<String, ServiceObject> classNameMap = new HashMap<>();

  private final MultiValueMap<ServiceMetadata, Class<?>> serviceMap = MultiValueMap.forLinkedHashMap();

  private final ServiceMetadataProvider serviceMetadataProvider;

  public LocalServiceHolder(ServiceMetadataProvider serviceMetadataProvider) {
    Assert.notNull(serviceMetadataProvider, "serviceMetadataProvider is required");
    this.serviceMetadataProvider = serviceMetadataProvider;
  }

  @Nullable
  @SuppressWarnings("unchecked")
  public <T> T getService(Class<T> serviceInterface) {
    return (T) localServices.get(serviceInterface);
  }

  @Nullable
  public ServiceObject getServiceInterface(String serviceClass) {
    return classNameMap.get(serviceClass);
  }

  @Override
  public List<ServiceMetadata> getServices() {
    return new ArrayList<>(serviceMap.keySet());
  }

  @Override
  public void afterSingletonsInstantiated() {
    ApplicationContext context = obtainApplicationContext();
    List<Object> services = context.getAnnotatedBeans(Service.class);

    for (Object service : services) {
      Class<Object> serviceImpl = ClassUtils.getUserClass(service);
      Set<Class<?>> interfaces = ClassUtils.getAllInterfacesForClassAsSet(serviceImpl);
      if (interfaces.isEmpty()) {
        continue;
      }

      for (final Class<?> anInterface : interfaces) {
        if (anInterface.isAnnotationPresent(Service.class)) {
          Object object = localServices.put(anInterface, service);
          String interfaceName = anInterface.getName();
          if (object != null) {
            throw new IllegalStateException("Service '%s' is already registered: [%s]".formatted(interfaceName, object));
          }

          ServiceMetadata serviceMetadata = serviceMetadataProvider.getMetadata(anInterface);
          serviceMap.add(serviceMetadata, anInterface);
          classNameMap.put(interfaceName, new ServiceObject(anInterface, service));
          logger.info("Adding service: [{}] to interface: [{}]", service, interfaceName);
        }
      }
    }
  }

}
