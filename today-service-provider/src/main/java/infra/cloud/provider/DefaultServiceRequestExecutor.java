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

import java.util.List;

import infra.cloud.service.InvocationResult;
import infra.cloud.service.InvocationResults;
import infra.cloud.service.InvocationType;
import infra.util.Assert;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Schedules decoded requests through an ordered {@link ServiceInterceptor} chain
 * and the method's pre-bound return value handler.
 *
 * <p>Each execution result starts once. The supplied request, its
 * argument array and local attributes are shared throughout the chain without
 * copying. Repeated observation of a single result does not repeat side effects.
 *
 * <p>The default scheduler is bounded-elastic to isolate blocking service methods.
 * An explicitly supplied scheduler is externally owned and is not disposed by this
 * executor. Cancellation propagates through the chain; stopping business work
 * still requires the service and interceptors to cooperate with cancellation.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2026/10/5
 */
public class DefaultServiceRequestExecutor implements ServiceRequestExecutor {

  private final Scheduler scheduler;

  private final DefaultInterceptorChain chain;

  /** Create an executor without interceptors using bounded-elastic scheduling. */
  public DefaultServiceRequestExecutor() {
    this(Schedulers.boundedElastic());
  }

  /**
   * Create an executor without interceptors on the supplied scheduler.
   *
   * @param scheduler the externally owned execution scheduler
   */
  public DefaultServiceRequestExecutor(Scheduler scheduler) {
    this(List.of(), scheduler);
  }

  /**
   * Create an executor using bounded-elastic scheduling.
   *
   * @param interceptors interceptors in outermost-first order, copied on construction
   */
  public DefaultServiceRequestExecutor(List<ServiceInterceptor> interceptors) {
    this(interceptors, Schedulers.boundedElastic());
  }

  /**
   * Create an executor with an ordered interceptor snapshot and explicit scheduling.
   *
   * @param interceptors interceptors in outermost-first order, never null and
   * containing no null elements
   * @param scheduler the externally owned execution scheduler, never null
   */
  public DefaultServiceRequestExecutor(List<ServiceInterceptor> interceptors, Scheduler scheduler) {
    Assert.notNull(scheduler, "scheduler is required");
    this.scheduler = scheduler;
    this.chain = new DefaultInterceptorChain(interceptors);
  }

  /**
   * Return a lazy single or streaming result for the supplied request.
   *
   * @param request the decoded service request, never null
   * @return a result emitting at most one adapted value, completing empty for null
   * or void, or failing with an invocation or interception error
   */
  @Override
  public InvocationResult execute(RemoteRequest request) {
    Assert.notNull(request, "request is required");
    var source = Flux.defer(() -> {
      try {
        return Flux.from(InvocationResults.publisher(chain.proceed(request)));
      }
      catch (Throwable error) {
        Exceptions.throwIfFatal(error);
        return Mono.error(error);
      }
    }).subscribeOn(scheduler);
    var adapter = request.getMethod().getResponseAdapter();
    return adapter != null && adapter.isMultiValue()
            ? InvocationResults.stream(InvocationType.RESPONSE_STREAMING, source) : InvocationResults.single(source);
  }

}
