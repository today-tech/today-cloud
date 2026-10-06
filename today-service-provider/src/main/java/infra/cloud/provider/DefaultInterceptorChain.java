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

import infra.cloud.service.InvocationResult;
import infra.cloud.service.InvocationResults;
import infra.cloud.service.InvocationType;
import reactor.core.publisher.Flux;
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

  private final @Nullable ServiceInterceptor interceptor;

  private final @Nullable DefaultInterceptorChain next;

  /**
   * Create a root chain with an ordered interceptor snapshot.
   *
   * @param interceptors interceptors in outermost-first order, never null and
   * containing no null elements
   */
  public DefaultInterceptorChain(List<ServiceInterceptor> interceptors) {
    DefaultInterceptorChain chain = new DefaultInterceptorChain(null, null);
    var iterator = interceptors.listIterator(interceptors.size());
    while (iterator.hasPrevious()) {
      chain = new DefaultInterceptorChain(iterator.previous(), chain);
    }
    this.interceptor = chain.interceptor;
    this.next = chain.next;
  }

  private DefaultInterceptorChain(@Nullable ServiceInterceptor interceptor, @Nullable DefaultInterceptorChain next) {
    this.interceptor = interceptor;
    this.next = next;
  }

  @Override
  public InvocationResult proceed(RemoteRequest request) throws Exception {
    if (interceptor != null && next != null) {
      return interceptor.intercept(request, next);
    }
    Publisher<Object> source = Flux.defer(() -> {
      try {
        Object value = request.invoke();
        return Flux.from(InvocationResults.publisher(request.getMethod().handleReturnValue(request, value)));
      }
      catch (Throwable error) {
        reactor.core.Exceptions.throwIfFatal(error);
        return Mono.error(error);
      }
    });
    var adapter = request.getMethod().getResponseAdapter();
    return adapter != null && adapter.isMultiValue()
            ? InvocationResults.stream(InvocationType.RESPONSE_STREAMING, source) : InvocationResults.single(source);
  }
}
