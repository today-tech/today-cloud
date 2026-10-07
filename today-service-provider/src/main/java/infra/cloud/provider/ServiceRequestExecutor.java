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
 * Transport-independent execution of a decoded service request.
 * Returns a stable single or streaming result handle. Observation does not start
 * execution. Single results start explicitly; streams start on data subscription.
 * Cancellation must propagate to the underlying invocation.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2026/10/5
 */
public interface ServiceRequestExecutor {

  InvocationResult execute(RemoteRequest request);
}
