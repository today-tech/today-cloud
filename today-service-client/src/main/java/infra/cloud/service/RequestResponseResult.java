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

import java.util.function.Function;

import infra.cloud.service.serialize.ResponseDeserializer;
import infra.remoting.Payload;
import infra.util.concurrent.Future;
import infra.util.concurrent.PublisherFuture;
import reactor.core.publisher.Mono;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/15 19:55
 */
final class RequestResponseResult extends AbstractInvocationResult implements Function<Payload, Object> {

  private final Mono<Payload> resultPublisher;

  private final ServiceInterfaceMethod method;

  private final ResponseDeserializer responseDeserializer;

  private Future<Object> future;

  RequestResponseResult(ServiceInterfaceMethod method, Mono<Payload> resultPublisher, ResponseDeserializer responseDeserializer) {
    this.resultPublisher = resultPublisher;
    this.method = method;
    this.responseDeserializer = responseDeserializer;
  }

  @Override
  public @Nullable Object getBlockingValue() {
    return publisher().block();
  }

  @Override
  public Object apply(Payload payload) {
    return responseDeserializer.deserialize(method, payload.data());
  }

  @Override
  public boolean isFailed() {
    return false;
  }

  @Override
  public Throwable getException() {
    return null;
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

  @Override
  public Future<Object> future() {
    if (future == null) {
      future = PublisherFuture.of(publisher());
    }
    return future;
  }

  @Override
  public Mono<Object> publisher() {
    return resultPublisher.map(this);
  }

}
