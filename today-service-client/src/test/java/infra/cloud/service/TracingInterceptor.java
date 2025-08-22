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
