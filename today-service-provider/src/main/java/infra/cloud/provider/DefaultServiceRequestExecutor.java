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

import infra.util.Assert;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Schedules decoded requests through an ordered {@link ServiceInterceptor} chain
 * and the method's pre-bound return value handler.
 *
 * <p>Each result subscription creates a fresh invocation and attribute context.
 * When interceptors are configured, the argument array is shallow-copied to isolate
 * element replacement between subscriptions. Referenced argument objects are not
 * copied. Each chain continuation is single-use, although subscribing to the
 * executor result again starts a new service invocation.
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

  private final List<ServiceInterceptor> interceptors;

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
    this.interceptors = List.copyOf(interceptors);
  }

  /**
   * Return a lazy result that starts a new invocation on each subscription.
   *
   * @param request the decoded service request, never null
   * @return a result emitting at most one adapted value, completing empty for null
   * or void, or failing with an invocation or interception error
   */
  @Override
  public Mono<Object> execute(RemoteRequest request) {
    Assert.notNull(request, "request is required");
    return Mono.defer(() -> {
              try {
                Object[] arguments = request.getArguments();
                RemoteRequest call = interceptors.isEmpty() ? request : new RemoteRequest(request.getMethod(),
                        arguments == null ? null : arguments.clone(), request.getServiceObject(), request.getMetadata());
                return Mono.from(new DefaultProviderInvocation(call, interceptors).proceed());
              }
              catch (Throwable error) {
                reactor.core.Exceptions.throwIfFatal(error);
                return Mono.error(error);
              }
            })
            .subscribeOn(scheduler);
  }

}
