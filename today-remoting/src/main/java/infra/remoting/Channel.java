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
 * A contract providing different interaction models for <a
 * href="https://github.com/today-tech/today-cloud/blob/master/today-remoting/Protocol.md">protocol</a>.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
public interface Channel extends Availability, Closeable, RemotingOperations {

  /**
   * Fire and Forget interaction model of protocol.
   *
   * @param payload Request payload.
   * @return {@code Publisher} that completes when the passed {@code payload} is successfully
   * handled, otherwise errors.
   */
  default Mono<Void> fireAndForget(Payload payload) {
    return ChannelAdapter.fireAndForget(payload);
  }

  /**
   * Request-Response interaction model of protocol.
   *
   * @param payload Request payload.
   * @return {@code Publisher} containing at most a single {@code Payload} representing the
   * response.
   */
  default Mono<Payload> requestResponse(Payload payload) {
    return ChannelAdapter.requestResponse(payload);
  }

  /**
   * Request-Stream interaction model of protocol.
   *
   * @param payload Request payload.
   * @return {@code Publisher} containing the stream of {@code Payload}s representing the response.
   */
  default Flux<Payload> requestStream(Payload payload) {
    return ChannelAdapter.requestStream(payload);
  }

  /**
   * Request-Channel interaction model of protocol.
   *
   * @param payloads Stream of request payloads.
   * @return Stream of response payloads.
   */
  default Flux<Payload> requestChannel(Publisher<Payload> payloads) {
    return ChannelAdapter.requestChannel(payloads);
  }

  /**
   * Metadata-Push interaction model of protocol.
   *
   * @param payload Request payloads.
   * @return {@code Publisher} that completes when the passed {@code payload} is successfully
   * handled, otherwise errors.
   */
  default Mono<Void> metadataPush(Payload payload) {
    return ChannelAdapter.metadataPush(payload);
  }

  @Override
  default double availability() {
    return isDisposed() ? 0.0 : 1.0;
  }

  @Override
  default void dispose() { }

  @Override
  default boolean isDisposed() {
    return false;
  }

  @Override
  default Mono<Void> onClose() {
    return Mono.never();
  }
}
