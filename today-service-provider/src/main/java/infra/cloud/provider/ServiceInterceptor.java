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

package infra.cloud.provider;

import infra.cloud.service.InvocationResult;

/**
 * Intercepts a decoded service invocation before response encoding.
 * Interceptors are applied in configured order: the first interceptor surrounds
 * all subsequent interceptors and the service method. Implementations are shared
 * between concurrent calls and must be thread-safe.
 *
 * <p>An interceptor may inspect or modify arguments, share per-call attributes,
 * delegate through {@link InterceptorChain#proceed(RemoteRequest)}, or short-circuit by
 * returning its own result handle. Results expose adapted service values rather than
 * asynchronous wrappers such as futures. Request decoding and response encoding
 * are outside this chain.
 *
 * <p>Completion, errors, and cancellation must be observed on the returned
 * result completion. Returning from {@link #intercept} does not indicate that the service
 * has finished. Wrappers must preserve downstream demand and cancellation.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2026/10/5
 */
@FunctionalInterface
public interface ServiceInterceptor {

  /**
   * Intercept one invocation and return its result handle.
   *
   * <p>Normally delegate once through {@link InterceptorChain#proceed(RemoteRequest)}.
   * Multiple calls obtain independent results and may repeat service side effects.
   * Returning without proceeding skips the remaining chain and service invocation.
   * Preserve the single or streaming capability when wrapping results.
   *
   * @param request the per-call context, including arguments, metadata and local attributes
   * @param chain the reusable continuation to the next interceptor or service method
   * @return a non-null result handle; observing completion does not start execution
   * @throws Exception if interception fails synchronously; the default executor
   * propagates non-fatal failures as error signals
   */
  InvocationResult intercept(RemoteRequest request, InterceptorChain chain)
          throws Exception;

}
