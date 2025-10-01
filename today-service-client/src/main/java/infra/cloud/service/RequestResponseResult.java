/*
 * Copyright 2021 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
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

  private final Mono<Payload> payloadMono;

  private final ServiceInterfaceMethod method;

  private final ResponseDeserializer responseDeserializer;

  RequestResponseResult(ServiceInterfaceMethod method, Mono<Payload> payloadMono, ResponseDeserializer responseDeserializer) {
    this.payloadMono = payloadMono;
    this.method = method;
    this.responseDeserializer = responseDeserializer;
  }

  @Nullable
  @Override
  public Object getValue() {
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
    return PublisherFuture.of(publisher());
  }

  @Override
  public Mono<Object> publisher() {
    return payloadMono.map(this);
  }

}
