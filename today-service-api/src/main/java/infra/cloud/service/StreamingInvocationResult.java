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

import org.reactivestreams.Publisher;

/**
 * Stream capability. One data subscription starts consumption and drives demand;
 * completion observation never creates another subscription. For duplex calls,
 * completion describes the output stream, not an independent input half-close.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2026/10/6
 */
public interface StreamingInvocationResult extends InvocationResult {

  /**
   * Obtain the output publisher without subscribing to its source.
   * A data subscription starts consumption; elements must be non-null and respect
   * demand. Cancellation terminates the invocation. A second subscription fails
   * rather than starting another stream.
   *
   * @return the same single-subscription publisher on every access
   */
  Publisher<Object> values();
}
