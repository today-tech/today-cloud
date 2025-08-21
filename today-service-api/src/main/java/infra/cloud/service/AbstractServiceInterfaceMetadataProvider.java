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

package infra.cloud.service;

import java.lang.reflect.Method;
import java.util.List;

import infra.core.MethodIntrospector;
import infra.lang.Assert;
import infra.util.ReflectionUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/10 16:53
 */
public abstract class AbstractServiceInterfaceMetadataProvider<M extends ServiceMethod> implements ServiceInterfaceMetadataProvider<M> {

  private final ServiceMetadataProvider serviceMetadataProvider;

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

  protected abstract M createServiceMethod(ServiceMetadata serviceMetadata, Class<?> serviceInterface, Method method);

  protected boolean isServiceMethod(Method method) {
    return !ReflectionUtils.isObjectMethod(method);
  }

}
