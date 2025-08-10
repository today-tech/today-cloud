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
