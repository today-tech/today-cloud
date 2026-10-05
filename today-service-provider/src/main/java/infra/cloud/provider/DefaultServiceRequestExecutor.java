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

import infra.util.Assert;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Executes service methods and delegates result adaptation to ordered handlers.
 * The supplied scheduler is externally owned and is not disposed by this executor.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2026/10/5
 */
public class DefaultServiceRequestExecutor implements ServiceRequestExecutor {

  private final Scheduler scheduler;

  public DefaultServiceRequestExecutor() {
    this(Schedulers.boundedElastic());
  }

  public DefaultServiceRequestExecutor(Scheduler scheduler) {
    Assert.notNull(scheduler, "scheduler is required");
    this.scheduler = scheduler;
  }

  @Override
  public Mono<Object> execute(RemoteRequest request) {
    Assert.notNull(request, "request is required");
    return Mono.defer(() -> {
              try {
                Object returnValue = request.invoke();
                return Mono.from(request.getMethod().handleReturnValue(request, returnValue));
              }
              catch (Throwable error) {
                reactor.core.Exceptions.throwIfFatal(error);
                return Mono.error(error);
              }
            })
            .subscribeOn(scheduler);
  }

}
