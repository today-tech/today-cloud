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

import infra.cloud.service.AbstractServiceInterfaceMetadataProvider;
import infra.cloud.service.ServiceMetadata;
import infra.cloud.service.ServiceMetadataProvider;
import infra.cloud.service.ServiceMethod;

/**
 * Default implementation of {@link AbstractServiceInterfaceMetadataProvider} that creates
 * {@link ServiceMethod} instances for service interface methods.
 *
 * <p>This provider is responsible for generating metadata representations of methods
 * defined in service interfaces, using the underlying {@link ServiceMetadataProvider}
 * to retrieve base service metadata.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/20 22:12
 */
public class DefaultServiceInterfaceMetadataProvider extends AbstractServiceInterfaceMetadataProvider<ServiceMethod> {

  /**
   * Constructs a new {@code DefaultServiceInterfaceMetadataProvider} with the specified
   * service metadata provider.
   *
   * @param serviceMetadataProvider the provider used to retrieve base service metadata
   */
  public DefaultServiceInterfaceMetadataProvider(ServiceMetadataProvider serviceMetadataProvider) {
    super(serviceMetadataProvider);
  }

  /**
   * Creates a new {@link ServiceMethod} instance for the given method of a service interface.
   *
   * @param serviceMetadata the base metadata of the service
   * @param serviceInterface the service interface class
   * @param method the method for which to create metadata
   * @return a new {@code ServiceMethod} instance
   */
  @Override
  protected ServiceMethod createServiceMethod(ServiceMetadata serviceMetadata, Class<?> serviceInterface, Method method) {
    return new ServiceMethod(serviceMetadata, serviceInterface, method);
  }

}
