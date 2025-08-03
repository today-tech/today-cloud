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
   * Fire and Forget interaction model of protocol.
   *
   * @param payload Request payload.
   * @return {@code Publisher} that completes when the passed {@code payload} is successfully
   * handled, otherwise errors.
   */
  Mono<Void> fireAndForget(Payload payload);

  /**
   * Request-Response interaction model of protocol.
   *
   * @param payload Request payload.
   * @return {@code Publisher} containing at most a single {@code Payload} representing the
   * response.
   */
  Mono<Payload> requestResponse(Payload payload);

  /**
   * Request-Stream interaction model of protocol.
   *
   * @param payload Request payload.
   * @return {@code Publisher} containing the stream of {@code Payload}s representing the response.
   */
  Flux<Payload> requestStream(Payload payload);

  /**
   * Request-Channel interaction model of protocol.
   *
   * @param payloads Stream of request payloads.
   * @return Stream of response payloads.
   */
  Flux<Payload> requestChannel(Publisher<Payload> payloads);

  /**
   * Metadata-Push interaction model of protocol.
   *
   * @param payload Request payloads.
   * @return {@code Publisher} that completes when the passed {@code payload} is successfully
   * handled, otherwise errors.
   */
  Mono<Void> metadataPush(Payload payload);

}
