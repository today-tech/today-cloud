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
