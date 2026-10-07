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
 * Client continuation independent of invocation context. Normally delegate once;
 * multiple calls may create independent remote calls and repeat side effects.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2026/10/6
 */
@FunctionalInterface
public interface InterceptorChain {

  /**
   * Proceed with the supplied context without interpreting its result lifecycle.
   * @param request the client invocation, including arguments and metadata
   * @return the invocation result; returning does not imply asynchronous completion
   * @throws Exception if interception or constructing the remote result fails
   */
  InvocationResult proceed(ClientRequest request) throws Exception;

}
