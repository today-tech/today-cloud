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
 * A contract providing different interaction models for <a
 * href="https://github.com/today-tech/today-cloud/blob/master/today-remoting/Protocol.md">protocol</a>.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
public interface Channel extends Availability, Closeable {

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
  default void dispose() {
  }

  @Override
  default Mono<Void> onClose() {
    return Mono.never();
  }
}
