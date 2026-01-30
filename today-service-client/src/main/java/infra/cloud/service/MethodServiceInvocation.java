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

import infra.core.AttributeAccessorSupport;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/9 14:11
 */
public abstract class MethodServiceInvocation extends AttributeAccessorSupport implements ServiceInvocation {

  protected final ServiceInterfaceMethod serviceMethod;

  private final Object[] args;

  private final ClientInterceptor[] interceptors;

  private int currentIndex = 0;

  private final int interceptorSize;

  public MethodServiceInvocation(ServiceInterfaceMethod serviceMethod, Object[] args, ClientInterceptor[] interceptors) {
    this.serviceMethod = serviceMethod;
    this.args = args;
    this.interceptors = interceptors;
    this.interceptorSize = interceptors.length;
  }

  @Override
  public InvocationResult proceed() throws Throwable {
    if (currentIndex < interceptorSize) {
      return interceptors[currentIndex++].intercept(this);
    }

    return invokeRemoting();
  }

  protected abstract InvocationResult invokeRemoting();

  @Override
  public ServiceInterfaceMethod getServiceMethod() {
    return serviceMethod;
  }

  @Override
  public ServiceMetadata getServiceMetadata() {
    return serviceMethod.serviceMetadata;
  }

  @Override
  public String getServiceId() {
    return serviceMethod.getServiceId();
  }

  @Override
  public Object[] getArguments() {
    return args;
  }

  @Override
  public InvocationType getType() {
    return serviceMethod.getInvocationType();
  }

}
