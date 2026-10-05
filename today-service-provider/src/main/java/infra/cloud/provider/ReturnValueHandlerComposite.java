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

import java.util.ArrayList;
import java.util.List;

import infra.cloud.service.ServiceMethod;
import infra.util.Assert;

/**
 * Selects a method's return value strategy. Ordered custom handlers precede
 * built-in handlers. Configure before resolving methods; handlers must be
 * thread-safe because the selected instance is shared by concurrent calls.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2026/10/5
 */
public final class ReturnValueHandlerComposite {

  private final List<ReturnValueHandler> handlers;

  public ReturnValueHandlerComposite(List<ReturnValueHandler> handlers) {
    Assert.notNull(handlers, "handlers is required");
    var strategies = new ArrayList<>(handlers);
    strategies.add(new ReactiveReturnValueHandler());
    strategies.add(new SimpleReturnValueHandler());
    this.handlers = List.copyOf(strategies);
  }

  public ReturnValueHandler select(ServiceMethod method) {
    for (ReturnValueHandler handler : handlers) {
      if (handler.supportsReturnValue(method)) {
        return handler;
      }
    }
    throw new IllegalStateException("No ReturnValueHandler for " + method);
  }
}
