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

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;

import infra.core.MethodParameter;

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
