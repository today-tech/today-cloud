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
