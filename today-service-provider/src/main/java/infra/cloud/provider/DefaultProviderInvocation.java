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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import infra.core.DefaultAttributeAccessor;
import infra.util.Assert;
import reactor.core.publisher.Mono;

/**
 * Default lazy, single-use implementation of {@link ProviderInvocation}.
 * Each chain node has its own atomic proceed guard, while all nodes share the
 * decoded request and per-call attributes. The interceptor list is snapshotted
 * when the root invocation is constructed.
 *
 * <p>The terminal continuation invokes the method and delegates to its pre-bound
 * return value handler. Non-fatal synchronous exceptions become error signals.
 * This implementation does not select an execution scheduler: the subscribing
 * executor controls execution. Argument and attribute access is not made
 * thread-safe by the atomic continuation guards.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2026/10/5
 */
public final class DefaultProviderInvocation extends DefaultAttributeAccessor implements ProviderInvocation {

  private final RemoteRequest request;

  private final List<ServiceInterceptor> interceptors;

  private final int index;

  private final AtomicBoolean proceeded = new AtomicBoolean();

  /**
   * Create a root invocation with fresh attributes and an ordered interceptor snapshot.
   * The request is retained as supplied; this constructor does not copy arguments.
   *
   * @param request the decoded request, never null
   * @param interceptors interceptors in outermost-first order, never null and
   * containing no null elements
   */
  public DefaultProviderInvocation(RemoteRequest request, List<ServiceInterceptor> interceptors) {
    this(request, List.copyOf(interceptors), 0, new HashMap<>());
  }

  private DefaultProviderInvocation(RemoteRequest request, List<ServiceInterceptor> interceptors,
          int index, Map<String, @Nullable Object> attributes) {
    Assert.notNull(request, "request is required");
    this.request = request;
    this.interceptors = interceptors;
    this.index = index;
    this.attributes = attributes;
  }

  @Override
  public RemoteRequest getRequest() {
    return request;
  }

  @Override
  public InvocableMethod getMethod() {
    return request.getMethod();
  }

  @Override
  public @Nullable Object @Nullable [] getArguments() {
    return request.getArguments();
  }

  /** {@inheritDoc} */
  @Override
  public Publisher<Object> proceed() {
    Assert.state(proceeded.compareAndSet(false, true), "proceed() may only be called once per interceptor");
    var subscribed = new AtomicBoolean();
    return Mono.defer(() -> {
      if (!subscribed.compareAndSet(false, true)) {
        return Mono.error(new IllegalStateException("Invocation continuation allows only one subscription"));
      }
      try {
        if (index < interceptors.size()) {
          ProviderInvocation next = new DefaultProviderInvocation(request, interceptors, index + 1, getAttributes());
          return Mono.from(interceptors.get(index).intercept(next));
        }
        Object value = request.invoke();
        return Mono.from(getMethod().handleReturnValue(request, value));
      }
      catch (Throwable error) {
        reactor.core.Exceptions.throwIfFatal(error);
        return Mono.error(error);
      }
    });
  }
}
