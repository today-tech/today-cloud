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

import infra.cloud.service.ServiceMethod;
import reactor.core.publisher.Mono;

/**
 * Fallback handler for ordinary values, null, and void.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2026/10/4
 */
public class SimpleReturnValueHandler implements ReturnValueHandler {

  @Override
  public boolean supportsReturnValue(ServiceMethod method) {
    return true;
  }

  @Override
  public Mono<Object> handleReturnValue(RemoteRequest request, @Nullable Object returnValue) {
    return Mono.justOrEmpty(returnValue);
  }
}
