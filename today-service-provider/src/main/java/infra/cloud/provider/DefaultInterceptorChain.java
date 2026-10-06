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

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import reactor.core.publisher.Mono;

/**
 * Immutable, reusable linked interceptor chain. The complete chain is built once
 * in reverse order and may serve concurrent requests. Request state is supplied
 * by the caller; subscription guards belong to returned publishers, not shared
 * nodes. Interceptors themselves must be thread-safe.
 *
 * <p>The terminal continuation invokes the method and delegates to its pre-bound
 * return value handler. Non-fatal synchronous exceptions become error signals.
 * This implementation does not select an execution scheduler: the subscribing
 * executor controls execution. Argument and attribute access is not made
 * thread-safe by the subscription guards.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2026/10/5
 */
public final class DefaultInterceptorChain implements InterceptorChain {

  private final List<ServiceInterceptor> interceptors;

  private final @Nullable ServiceInterceptor interceptor;

  private final @Nullable DefaultInterceptorChain next;

  /**
   * Create a root chain with an ordered interceptor snapshot.
   *
   * @param interceptors interceptors in outermost-first order, never null and
   * containing no null elements
   */
  public DefaultInterceptorChain(List<ServiceInterceptor> interceptors) {
    this.interceptors = interceptors;
    DefaultInterceptorChain chain = new DefaultInterceptorChain(this.interceptors, null, null);
    var iterator = this.interceptors.listIterator(this.interceptors.size());
    while (iterator.hasPrevious()) {
      chain = new DefaultInterceptorChain(this.interceptors, iterator.previous(), chain);
    }
    this.interceptor = chain.interceptor;
    this.next = chain.next;
  }

  private DefaultInterceptorChain(List<ServiceInterceptor> interceptors,
          @Nullable ServiceInterceptor interceptor, @Nullable DefaultInterceptorChain next) {
    this.interceptors = interceptors;
    this.interceptor = interceptor;
    this.next = next;
  }

  /** Return the immutable interceptor snapshot in outermost-first order. */
  public List<ServiceInterceptor> getInterceptors() {
    return interceptors;
  }

  @Override
  public Publisher<Object> proceed(RemoteRequest request) {
    var subscribed = new AtomicBoolean();
    return Mono.defer(() -> {
      if (!subscribed.compareAndSet(false, true)) {
        return Mono.error(new IllegalStateException("Invocation continuation allows only one subscription"));
      }
      try {
        if (interceptor != null && next != null) {
          return Mono.from(interceptor.intercept(request, next));
        }
        Object value = request.invoke();
        return Mono.from(request.getMethod().handleReturnValue(request, value));
      }
      catch (Throwable error) {
        reactor.core.Exceptions.throwIfFatal(error);
        return Mono.error(error);
      }
    });
  }
}
