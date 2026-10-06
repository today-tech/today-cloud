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

import org.reactivestreams.Publisher;

/**
 * Continuation independent of request context. The default chain is immutable
 * and reusable. Each proceed call obtains a new lazy result; interceptors normally
 * delegate once and are responsible for side effects of multiple executions.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2026/10/6
 */
public interface InterceptorChain {

  /**
   * Obtain a lazy continuation for the supplied request. The default implementation
   * advances on subscription, emitting at most one adapted value or completing
   * empty for null and void. Wrappers must preserve cancellation and demand.
   *
   * @param request context passed to the next interceptor or terminal invocation
   * @return non-null result publisher permitting only one subscription
   * @throws Exception if obtaining the continuation fails synchronously
   */
  Publisher<Object> proceed(RemoteRequest request) throws Exception;

}
