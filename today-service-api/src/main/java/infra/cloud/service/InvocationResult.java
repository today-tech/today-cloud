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
import org.reactivestreams.Publisher;

import infra.core.AttributeAccessor;
import infra.util.concurrent.Future;

/**
 * Remote service invocation result
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/9 12:10
 */
public interface InvocationResult extends AttributeAccessor {

  @Nullable
  Object getValue();

  boolean isFailed();

  @Nullable
  Throwable getException();

  InvocationType getType();

  default boolean isRequestResponse() {
    return getType() == InvocationType.REQUEST_RESPONSE;
  }

  default boolean isStreaming() {
    return !getType().serverSendsOneMessage();
  }

  /**
   * Only for {@link InvocationType#REQUEST_RESPONSE}
   *
   * @see InvocationType#REQUEST_RESPONSE
   * @see #isRequestResponse()
   */
  Future<Object> future();

  Publisher<Object> publisher();

}
