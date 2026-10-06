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
import reactor.core.publisher.Mono;

/**
 * Completion-only client result for a fire-and-forget request.
 *
 * <p>Execution starts explicitly and at most once. Completion describes the local
 * send operation; it does not acknowledge successful execution by the remote
 * service. Cancellation is forwarded to the underlying send operation.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/15 20:36
 */
final class FireAndForgetResult implements InvocationResult {

  private final InvocationResult delegate;

  FireAndForgetResult(Mono<Void> source) {
    delegate = InvocationResults.completion(source);
  }

  public InvocationType getType() {
    return delegate.getType();
  }

  public Future<Void> completion() {
    return delegate.completion();
  }

  public void start() {
    delegate.start();
  }

  public boolean cancel() {
    return delegate.cancel();
  }
}
