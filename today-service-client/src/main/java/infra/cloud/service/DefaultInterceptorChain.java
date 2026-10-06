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

import java.util.List;

import infra.util.Assert;

/**
 * Immutable client chain built once in reverse order. Nodes contain no per-call
 * cursor or context and can serve concurrent invocations. Interceptors and the
 * terminal operation must be thread-safe; invocation context is owned by each call.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2026/10/6
 */
public final class DefaultInterceptorChain implements InterceptorChain {

  private final @Nullable ClientInterceptor interceptor;

  private final InterceptorChain next;

  /**
   * Build a chain in outermost-first order, without retaining the supplied list.
   *
   * @param interceptors client interceptors, not modified during construction
   * @param terminal the terminal remote operation
   */
  public DefaultInterceptorChain(List<ClientInterceptor> interceptors, InterceptorChain terminal) {
    Assert.notNull(terminal, "terminal is required");
    InterceptorChain chain = terminal;
    var iterator = interceptors.listIterator(interceptors.size());
    while (iterator.hasPrevious()) {
      ClientInterceptor interceptor = iterator.previous();
      Assert.notNull(interceptor, "interceptor is required");
      chain = new DefaultInterceptorChain(interceptor, chain);
    }
    this.interceptor = null;
    this.next = chain;
  }

  private DefaultInterceptorChain(@Nullable ClientInterceptor interceptor, InterceptorChain next) {
    this.interceptor = interceptor;
    this.next = next;
  }

  @Override
  public InvocationResult proceed(ClientRequest invocation) throws Throwable {
    return interceptor != null ? interceptor.intercept(invocation, next) : next.proceed(invocation);
  }

}
