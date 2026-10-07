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

import org.jspecify.annotations.Nullable;

import infra.util.concurrent.Future;

/**
 * Lifecycle handle for one client or provider invocation.
 *
 * <p>Data access is defined by {@link SingleInvocationResult} and
 * {@link StreamingInvocationResult}. Completion-only operations expose neither
 * capability. Observing a future does not start execution or create an additional
 * data subscription. This handle is a local abstraction, not a wire response.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/9 12:10
 */
public interface InvocationResult {

  /**
   * Return the stable future representing termination of this invocation.
   * Accessing the future or registering a listener does not start execution or
   * subscribe to the data source. Cancelling the future cancels the invocation.
   *
   * @return the same future on every access, succeeding with null on completion,
   * failing on invocation error, or cancelled when the invocation is cancelled
   * @see #start()
   * @see #cancel()
   */
  Future<Void> completion();

  /**
   * Start a single-value or completion-only operation at most once.
   * Subsequent calls are no-ops. For streaming results this method does not consume
   * data: the data subscriber starts consumption and controls demand.
   * Calling this method after cancellation must not start the underlying work.
   * Starting an operation does not imply that it has completed.
   *
   * @see SingleInvocationResult#value()
   * @see StreamingInvocationResult#values()
   */
  void start();

  /**
   * Cancel the invocation and its associated asynchronous work.
   * Cancellation may precede execution and does not roll back business side effects.
   * Stopping already-running work requires the underlying operation to cooperate
   * with cancellation. Cancellation is a terminal state, distinct from success.
   *
   * @return {@code true} if this call transitions the result to cancelled,
   * {@code false} if it has already terminated
   */
  boolean cancel();

  /**
   * Determine whether termination represents failure or cancellation.
   * A false result does not distinguish pending execution from success.
   *
   * @return whether the completion future represents failure or cancellation
   * @see Future#isFailed()
   */
  default boolean isFailed() {
    return completion().isFailed();
  }

  /**
   * Return the terminal failure or cancellation cause, if available.
   *
   * @return the cause, or null while pending or after successful completion
   */
  default @Nullable Throwable getException() {
    return completion().getCause();
  }

  /**
   * Return the interaction model represented by this result.
   *
   * @return the invocation type
   */
  InvocationType getType();

  /**
   * Determine whether this result represents a request-response invocation.
   *
   * @return {@code true} for {@link InvocationType#REQUEST_RESPONSE}
   */
  default boolean isRequestResponse() {
    return getType() == InvocationType.REQUEST_RESPONSE;
  }

  /**
   * Determine whether the interaction model has a streaming output.
   * Fire-and-forget operations are not streaming invocations.
   *
   * @return {@code true} for response-streaming or duplex-streaming invocations
   * @see StreamingInvocationResult
   */
  default boolean isStreaming() {
    return getType() == InvocationType.RESPONSE_STREAMING
            || getType() == InvocationType.DUPLEX_STREAMING;
  }

}
