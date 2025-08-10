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

import java.util.Collections;
import java.util.List;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/9 21:59
 */
public class ServiceInterfaceMetadata<M extends ServiceMethod> {

  private final ServiceMetadata serviceMetadata;

  private final Class<?> serviceInterface;

  private final List<M> serviceMethods;

  public ServiceInterfaceMetadata(Class<?> serviceInterface, ServiceMetadata serviceMetadata, List<M> serviceMethods) {
    this.serviceInterface = serviceInterface;
    this.serviceMetadata = serviceMetadata;
    this.serviceMethods = serviceMethods;
  }

  public Class<?> getServiceInterface() {
    return serviceInterface;
  }

  public ServiceMetadata getServiceMetadata() {
    return serviceMetadata;
  }

  public List<M> getServiceMethods() {
    return Collections.unmodifiableList(serviceMethods);
  }

}
