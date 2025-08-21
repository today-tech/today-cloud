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

import infra.cloud.service.AbstractServiceInterfaceMetadataProvider;
import infra.cloud.service.ServiceMetadata;
import infra.cloud.service.ServiceMetadataProvider;
import infra.cloud.service.ServiceMethod;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/20 22:12
 */
public class DefaultServiceInterfaceMetadataProvider extends AbstractServiceInterfaceMetadataProvider<ServiceMethod> {

  public DefaultServiceInterfaceMetadataProvider(ServiceMetadataProvider serviceMetadataProvider) {
    super(serviceMetadataProvider);
  }

  @Override
  protected ServiceMethod createServiceMethod(ServiceMetadata serviceMetadata, Class<?> serviceInterface, Method method) {
    return new ServiceMethod(serviceMetadata, serviceInterface, method);
  }

}
