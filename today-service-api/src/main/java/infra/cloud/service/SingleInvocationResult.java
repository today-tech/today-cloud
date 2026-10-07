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

import infra.util.concurrent.Future;

/**
 * Single-value capability. The same future is returned on every access without
 * starting work. Successful null represents an empty or void result.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2026/10/6
 */
public interface SingleInvocationResult extends InvocationResult {

  /**
   * Obtain the stable value future without starting execution.
   * Call {@link #start()} to execute. Cancelling this future cancels the invocation.
   * Repeated access observes the same value and never creates a new invocation.
   *
   * @return the same future on every access; its successful value may be null
   */
  Future<Object> value();
}
