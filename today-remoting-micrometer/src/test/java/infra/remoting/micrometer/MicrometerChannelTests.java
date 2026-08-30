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

package infra.remoting.micrometer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import infra.remoting.Channel;
import infra.remoting.Payload;
import infra.remoting.util.DefaultPayload;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class MicrometerChannelTests {

  private final Channel delegate = mock(Channel.class, RETURNS_SMART_NULLS);

  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

  @DisplayName("constructor throws NullPointerException with null delegate")
  @Test
  void constructorNullDelegate() {
    assertThatNullPointerException()
            .isThrownBy(() -> new MicrometerChannel(null, meterRegistry))
            .withMessage("delegate is required");
  }

  @DisplayName("constructor throws NullPointerException with null meterRegistry")
  @Test
  void constructorNullMeterRegistry() {
    assertThatNullPointerException()
            .isThrownBy(() -> new MicrometerChannel(delegate, null))
            .withMessage("meterRegistry is required");
  }

  @DisplayName("fireAndForget gathers metrics")
  @Test
  void fireAndForget() {
    Payload payload = DefaultPayload.create("test-metadata", "test-data");
    when(delegate.fireAndForget(payload)).thenReturn(Mono.empty());

    new MicrometerChannel(delegate, meterRegistry, Tag.of("test-key", "test-value"))
            .fireAndForget(payload)
            .as(StepVerifier::create)
            .verifyComplete();

    assertThat(findCounter("request.fnf", SignalType.ON_COMPLETE).count()).isEqualTo(1);
  }

  @DisplayName("metadataPush gathers metrics")
  @Test
  void metadataPush() {
    Payload payload = DefaultPayload.create("test-metadata", "test-data");
    when(delegate.metadataPush(payload)).thenReturn(Mono.empty());

    new MicrometerChannel(delegate, meterRegistry, Tag.of("test-key", "test-value"))
            .metadataPush(payload)
            .as(StepVerifier::create)
            .verifyComplete();

    assertThat(findCounter("metadata.push", SignalType.ON_COMPLETE).count()).isEqualTo(1);
  }

  @DisplayName("requestChannel gathers metrics")
  @Test
  void requestChannel() {
    Mono<Payload> payload = Mono.just(DefaultPayload.create("test-metadata", "test-data"));
    when(delegate.requestChannel(payload)).thenReturn(Flux.empty());

    new MicrometerChannel(delegate, meterRegistry, Tag.of("test-key", "test-value"))
            .requestChannel(payload)
            .as(StepVerifier::create)
            .verifyComplete();

    assertThat(findCounter("request.channel", SignalType.ON_COMPLETE).count()).isEqualTo(1);
  }

  @DisplayName("requestResponse gathers metrics")
  @Test
  void requestResponse() {
    Payload payload = DefaultPayload.create("test-metadata", "test-data");
    when(delegate.requestResponse(payload)).thenReturn(Mono.empty());

    new MicrometerChannel(delegate, meterRegistry, Tag.of("test-key", "test-value"))
            .requestResponse(payload)
            .as(StepVerifier::create)
            .verifyComplete();

    assertThat(findTimer("request.response", SignalType.ON_COMPLETE).count()).isEqualTo(1);
  }

  @DisplayName("requestStream gathers metrics")
  @Test
  void requestStream() {
    Payload payload = DefaultPayload.create("test-metadata", "test-data");
    when(delegate.requestStream(payload)).thenReturn(Flux.empty());

    new MicrometerChannel(delegate, meterRegistry, Tag.of("test-key", "test-value"))
            .requestStream(payload)
            .as(StepVerifier::create)
            .verifyComplete();

    assertThat(findCounter("request.stream", SignalType.ON_COMPLETE).count()).isEqualTo(1);
  }

  private Counter findCounter(String interactionModel, SignalType signalType) {
    return meterRegistry
            .get(String.format("infra.remoting.%s", interactionModel))
            .tag("signal.type", signalType.name())
            .tag("test-key", "test-value")
            .counter();
  }

  private Timer findTimer(String interactionModel, SignalType signalType) {
    return meterRegistry
            .get(String.format("infra.remoting.%s", interactionModel))
            .tag("signal.type", signalType.name())
            .tag("test-key", "test-value")
            .timer();
  }
}
