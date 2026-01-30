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

import org.reactivestreams.Publisher;

import infra.remoting.Payload;
import infra.util.concurrent.Future;
import reactor.core.publisher.Flux;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/15 20:35
 */
class DuplexStreamingResult extends AbstractInvocationResult {

  private final Flux<Payload> payloadFlux;

  public DuplexStreamingResult(Flux<Payload> payloadFlux) {
    this.payloadFlux = payloadFlux;
  }

  @Override
  public Object getValue() {
    return null;
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
    return InvocationType.DUPLEX_STREAMING;
  }

  @Override
  public boolean isRequestResponse() {
    return false;
  }

  @Override
  public boolean isStreaming() {
    return true;
  }

  @Override
  public Future<Object> future() {
    return null;
  }

  @Override
  public Publisher<Object> publisher() {
    return null;
  }
}
