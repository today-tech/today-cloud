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
import java.util.ArrayList;

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
