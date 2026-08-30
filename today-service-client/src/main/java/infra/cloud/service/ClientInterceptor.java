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
 * Intercepts calls on an interface on its way to the target. These are nested
 * "on top" of the target.
 *
 * <p>
 * The user should implement the {@link #intercept(ServiceInvocation)} method to
 * modify the original behavior. E.g. the following class implements a tracing
 * interceptor (traces all the calls on the intercepted method(s)):
 *
 * <pre>{@code
 * class TracingInterceptor implements ClientInterceptor {
 *    public InvocationResult intercept(ServiceInvocation i) throws Throwable {
 *         System.out.println("before service " + i + " with args " + i.getArguments());
 *         Object ret = i.proceed();
 *         System.out.println("after service " + i + " returns " + ret);
 *         return ret;
 *     }
 * }
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/9 15:39
 */
public interface ClientInterceptor {

  InvocationResult intercept(ServiceInvocation invocation) throws Throwable;

}
