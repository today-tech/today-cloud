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

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/10 08:20
 */
public class ServiceMethod {

  protected final ServiceMetadata serviceMetadata;

  protected final Class<?> serviceInterface;

  protected final Method method;

  public ServiceMethod(ServiceMetadata serviceMetadata, Class<?> serviceInterface, Method method) {
    this.serviceMetadata = serviceMetadata;
    this.serviceInterface = serviceInterface;
    this.method = method;
  }

  public ServiceMetadata getServiceMetadata() {
    return serviceMetadata;
  }

  public Class<?> getServiceInterface() {
    return serviceInterface;
  }

  public Method getMethod() {
    return method;
  }

}
