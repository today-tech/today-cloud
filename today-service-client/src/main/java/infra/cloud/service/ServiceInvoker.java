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

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/9 12:01
 */
public interface ServiceInvoker {

  /**
   * Implement this method to perform extra treatments before and after the
   * invocation. Polite implementations would certainly like to invoke
   * {@link ServiceInvocation#proceed()}.
   *
   * @param serviceMethod the service method
   * @param args invocation args
   * @return the result of the call to {@link ServiceInvocation#proceed()}, might be
   * intercepted by the interceptor.
   * @throws Throwable if the interceptors or the target-object throws an exception.
   */
  InvocationResult invoke(ServiceInterfaceMethod serviceMethod, Object[] args) throws Throwable;

}
