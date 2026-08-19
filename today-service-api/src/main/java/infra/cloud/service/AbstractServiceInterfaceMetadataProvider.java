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

package infra.cloud.service;

import java.lang.reflect.Method;
import java.util.List;

import infra.core.MethodIntrospector;
import infra.util.Assert;
import infra.util.ReflectionUtils;

/**
 * Abstract base class for providing metadata about service interfaces.
 * <p>This class implements the {@link ServiceInterfaceMetadataProvider} interface and provides
 * a common implementation for extracting service method metadata from a given service interface.
 * It relies on a {@link ServiceMetadataProvider} to obtain general service metadata and uses
 * reflection to identify and process service methods.
 *
 * @param <M> the type of the service method, which must extend {@link ServiceMethod}
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/10 16:53
 */
public abstract class AbstractServiceInterfaceMetadataProvider<M extends ServiceMethod> implements ServiceInterfaceMetadataProvider<M> {

  private final ServiceMetadataProvider serviceMetadataProvider;

  /**
   * Constructs a new {@code AbstractServiceInterfaceMetadataProvider} with the specified
   * {@link ServiceMetadataProvider}.
   *
   * @param serviceMetadataProvider the provider for general service metadata; must not be null
   * @throws IllegalArgumentException if {@code serviceMetadataProvider} is null
   */
  protected AbstractServiceInterfaceMetadataProvider(ServiceMetadataProvider serviceMetadataProvider) {
    Assert.notNull(serviceMetadataProvider, "serviceMetadataProvider is required");
    this.serviceMetadataProvider = serviceMetadataProvider;
  }

  @Override
  public ServiceInterfaceMetadata<M> getMetadata(Class<?> serviceInterface) {
    ServiceMetadata serviceMetadata = serviceMetadataProvider.getMetadata(serviceInterface);

    List<M> serviceMethods = MethodIntrospector.filterMethods(serviceInterface, this::isServiceMethod).stream()
            .map(method -> createServiceMethod(serviceMetadata, serviceInterface, method))
            .toList();

    return new ServiceInterfaceMetadata<>(serviceInterface, serviceMetadata, serviceMethods);
  }

  /**
   * Creates a specific service method instance based on the provided service metadata,
   * service interface, and reflected method.
   *
   * @param serviceMetadata the general metadata of the service
   * @param serviceInterface the service interface class
   * @param method the reflected method to be wrapped
   * @return a new instance of {@code M} representing the service method
   */
  protected abstract M createServiceMethod(ServiceMetadata serviceMetadata, Class<?> serviceInterface, Method method);

  /**
   * Determines whether the given method should be considered a service method.
   * <p>By default, this implementation excludes standard {@link Object} methods.
   *
   * @param method the method to check
   * @return {@code true} if the method is a service method, {@code false} otherwise
   */
  protected boolean isServiceMethod(Method method) {
    return !ReflectionUtils.isObjectMethod(method);
  }

}
