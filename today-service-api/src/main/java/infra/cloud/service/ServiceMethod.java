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

import infra.core.MethodParameter;
import infra.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/10 08:20
 */
public class ServiceMethod {

  protected final ServiceMetadata serviceMetadata;

  protected final MethodParameter[] parameters;

  protected final Class<?> serviceInterface;

  protected final Method method;

  @Nullable
  private MethodParameter returnTypeParameter;

  public ServiceMethod(ServiceMetadata serviceMetadata, Class<?> serviceInterface, Method method) {
    this.parameters = initMethodParameters(method);
    this.serviceInterface = serviceInterface;
    this.serviceMetadata = serviceMetadata;
    this.method = method;
  }

  public String getServiceId() {
    return serviceMetadata.getId();
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

  public MethodParameter[] getParameters() {
    return parameters;
  }

  public MethodParameter getReturnType() {
    MethodParameter returnType = returnTypeParameter;
    if (returnType == null) {
      returnType = MethodParameter.forExecutable(method, -1);
      this.returnTypeParameter = returnType;
    }
    return returnType;
  }

  private MethodParameter[] initMethodParameters(Method method) {
    int count = method.getParameterCount();
    if (count == 0) {
      return MethodParameter.EMPTY_ARRAY;
    }

    MethodParameter[] result = new MethodParameter[count];
    for (int i = 0; i < count; i++) {
      result[i] = new MethodParameter(method, i);
    }
    return result;
  }

}
