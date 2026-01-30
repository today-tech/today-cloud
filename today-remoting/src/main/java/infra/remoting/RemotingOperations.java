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

package infra.remoting;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/2 23:10
 */
public interface RemotingOperations {

  /**
   * Perform a Fire-and-Forget interaction via {@link Channel#fireAndForget(Payload)}. Allows
   * multiple subscriptions and performs a request per subscriber.
   */
  Mono<Void> fireAndForget(Mono<Payload> payloadMono);

  /**
   * Perform a Request-Response interaction via {@link Channel#requestResponse(Payload)}. Allows
   * multiple subscriptions and performs a request per subscriber.
   */
  Mono<Payload> requestResponse(Mono<Payload> payloadMono);

  /**
   * Perform a Request-Stream interaction via {@link Channel#requestStream(Payload)}. Allows
   * multiple subscriptions and performs a request per subscriber.
   */
  Flux<Payload> requestStream(Mono<Payload> payloadMono);

  /**
   * Perform a Request-Channel interaction via {@link Channel#requestChannel(Publisher)}. Allows
   * multiple subscriptions and performs a request per subscriber.
   */
  Flux<Payload> requestChannel(Publisher<Payload> payloads);

  /**
   * Perform a Metadata Push via {@link Channel#metadataPush(Payload)}. Allows multiple
   * subscriptions and performs a request per subscriber.
   */
  Mono<Void> metadataPush(Mono<Payload> payloadMono);

}
