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

import java.util.Arrays;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/13 16:56
 */
class TracingInterceptor implements ClientInterceptor {

  @Override
  public InvocationResult intercept(ServiceInvocation invocation) throws Throwable {
    System.out.printf("before service %s with args %s%n", invocation, Arrays.toString(invocation.getArguments()));
    InvocationResult result = invocation.proceed();

    if (result.isRequestResponse()) {
      result.future().onCompleted(future -> {
        if (future.isSuccess()) {
          System.out.printf("after service %s returns %s%n", invocation, future.getNow());
        }
        else {
          System.out.printf("service %s failed %s%n", invocation, future.getCause());
        }
      });
    }
    return result;
  }

}
