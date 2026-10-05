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

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;

import infra.core.AttributeAccessor;

/**
 * Per-call context and single-use continuation for a provider interceptor.
 * Arguments may be modified before proceeding. Attributes are shared across the
 * chain, but not across calls; access must be serialized by the interceptors.
 * Attributes are accessed directly through {@link AttributeAccessor}, for example
 * {@link #setAttribute(String, Object)} and {@link #getAttribute(String)}. They are
 * local to the invocation and are not transmitted onto the wire.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2026/10/5
 */
public interface ProviderInvocation extends AttributeAccessor {

  /**
   * Return the decoded request shared by this invocation's chain.
   *
   * @return the request containing the bound method, target service, and arguments
   */
  RemoteRequest getRequest();

  /**
   * Return the resolved method and its pre-bound return value handler.
   * The method is reusable across calls and must not hold per-call state.
   *
   * @return the invocation method
   */
  InvocableMethod getMethod();

  /**
   * Return the live argument array of {@link #getRequest()}.
   * Elements may be replaced before proceeding; changes are visible to subsequent
   * interceptors and the service method. The default executor shallow-copies the
   * array for intercepted calls but does not copy argument objects themselves.
   *
   * @return the arguments, possibly null for a no-argument method; elements may be null
   */
  @Nullable Object @Nullable [] getArguments();

  /**
   * Obtain the continuation for the next interceptor or the service method.
   *
   * <p>This continuation may be obtained only once per chain node and subscribed
   * to only once. Advancing is lazy: the default implementation executes the next
   * stage on subscription, not when this method returns. Re-subscribing the
   * continuation for retry is unsupported; it could repeat business side effects.
   *
   * @return a non-null publisher of at most one adapted result; null and void
   * results complete empty, and cancellation propagates to the underlying work
   * @throws IllegalStateException if this node has already proceeded; duplicate
   * subscriptions are rejected through an error signal
   * @throws Throwable if the continuation cannot be obtained synchronously
   */
  Publisher<Object> proceed() throws Throwable;

}
