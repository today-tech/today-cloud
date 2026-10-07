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

import infra.cloud.service.InvocationResult;
import infra.cloud.service.InvocationResults;
import infra.cloud.service.InvocationType;
import infra.cloud.service.ServiceMethod;
import infra.core.ReactiveAdapter;
import infra.util.Assert;

/**
 * Handles asynchronous values such as Mono, Future, and CompletionStage.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2026/10/4
 */
public class ReactiveReturnValueHandler implements ReturnValueHandler {

  @Override
  public boolean supportsReturnValue(ServiceMethod method) {
    return method.getResponseAdapter() != null;
  }

  @Override
  public InvocationResult handleReturnValue(RemoteRequest request, @Nullable Object returnValue) {
    ReactiveAdapter adapter = request.getMethod().getResponseAdapter();
    Assert.state(adapter != null, "No reactive adapter for service return type");
    if (adapter.isMultiValue()) {
      return InvocationResults.stream(InvocationType.RESPONSE_STREAMING, adapter.toPublisher(returnValue));
    }
    return InvocationResults.single(adapter.toPublisher(returnValue));
  }
}
