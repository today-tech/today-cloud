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

import infra.cloud.service.serialize.ResponseDeserializer;
import infra.remoting.Payload;
import infra.util.concurrent.Future;
import reactor.core.publisher.Mono;

/**
 * Client-side single result that decodes and releases one response payload.
 *
 * <p>Value and completion futures do not start network work on access. Explicit
 * start or result bridging starts the request once. A decoded null completes the
 * value future successfully with null. The payload is released even if decoding fails.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/15 19:55
 */
final class RequestResponseResult extends AbstractInvocationResult implements SingleInvocationResult {

  private final Mono<Payload> resultPublisher;

  private final ServiceInterfaceMethod method;

  private final ResponseDeserializer responseDeserializer;

  private final SingleInvocationResult delegate;

  RequestResponseResult(ServiceInterfaceMethod method, Mono<Payload> resultPublisher, ResponseDeserializer responseDeserializer) {
    this.resultPublisher = resultPublisher;
    this.method = method;
    this.responseDeserializer = responseDeserializer;
    this.delegate = InvocationResults.single(resultPublisher.handle((payload, sink) -> {
      Object value = apply(payload);
      if (value != null) {
        sink.next(value);
      }
    }));
  }

  @Override
  public boolean isFailed() {
    return delegate.isFailed();
  }

  @Override
  public @Nullable Throwable getException() {
    return delegate.getException();
  }

  @Override
  public InvocationType getType() {
    return InvocationType.REQUEST_RESPONSE;
  }

  @Override
  public boolean isRequestResponse() {
    return true;
  }

  @Override
  public boolean isStreaming() {
    return false;
  }

  public Future<Object> value() {
    return delegate.value();
  }

  public Future<Void> completion() {
    return delegate.completion();
  }

  public void start() {
    delegate.start();
  }

  public boolean cancel() {
    return delegate.cancel();
  }

  private Object apply(Payload payload) {
    try {
      return responseDeserializer.deserialize(method, payload.data());
    }
    finally {
      payload.release();
    }
  }

}
