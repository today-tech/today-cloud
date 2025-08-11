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

import infra.core.AttributeAccessorSupport;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/9 14:11
 */
public abstract class MethodServiceInvocation extends AttributeAccessorSupport implements ServiceInvocation {

  private final ServiceInterfaceMethod serviceMethod;

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

    return invokeRemoting(args);
  }

  protected abstract InvocationResult invokeRemoting(Object[] args);

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
