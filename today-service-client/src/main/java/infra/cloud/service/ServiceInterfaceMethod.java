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
import java.util.ArrayList;

import infra.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/1/7 21:03
 */
public class ServiceInterfaceMethod extends ServiceMethod {

  private final InvocationType invocationType;

  private final ReturnValueResolver returnValueResolver;

  private final boolean blocking;

  ServiceInterfaceMethod(ServiceMetadata serviceMetadata, Class<?> serviceInterface, Method method, ArrayList<ReturnValueResolver> resolvers) {
    super(serviceMetadata, serviceInterface, method);
    this.returnValueResolver = findReturnValueResolver(resolvers);
    this.blocking = returnValueResolver.isBlocking();
    this.invocationType = returnValueResolver.getInvocationType(this);
  }

  public boolean isBlocking() {
    return blocking;
  }

  public InvocationType getInvocationType() {
    return invocationType;
  }

  @Nullable
  public Object resolveResult(InvocationResult result) throws Throwable {
    return returnValueResolver.resolve(this, result);
  }

  private ReturnValueResolver findReturnValueResolver(ArrayList<ReturnValueResolver> resolvers) {
    for (ReturnValueResolver resolver : resolvers) {
      if (resolver.supportsMethod(this)) {
        return resolver;
      }
    }
    throw new IllegalArgumentException("No ReturnValueResolver found for method: " + method);
  }

}
