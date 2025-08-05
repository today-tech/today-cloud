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
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import infra.remoting.Connection;
import infra.remoting.frame.FrameType;
import infra.remoting.plugins.ConnectionDecorator.Type;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.test.StepVerifier;

import static infra.remoting.frame.FrameType.CANCEL;
import static infra.remoting.frame.FrameType.COMPLETE;
import static infra.remoting.frame.FrameType.ERROR;
import static infra.remoting.frame.FrameType.KEEPALIVE;
import static infra.remoting.frame.FrameType.LEASE;
import static infra.remoting.frame.FrameType.METADATA_PUSH;
import static infra.remoting.frame.FrameType.REQUEST_CHANNEL;
import static infra.remoting.frame.FrameType.REQUEST_FNF;
import static infra.remoting.frame.FrameType.REQUEST_N;
import static infra.remoting.frame.FrameType.REQUEST_RESPONSE;
import static infra.remoting.frame.FrameType.REQUEST_STREAM;
import static infra.remoting.frame.FrameType.SETUP;
import static infra.remoting.plugins.ConnectionDecorator.Type.CLIENT;
import static infra.remoting.plugins.ConnectionDecorator.Type.SERVER;
import static infra.remoting.test.TestFrames.createTestCancelFrame;
import static infra.remoting.test.TestFrames.createTestErrorFrame;
import static infra.remoting.test.TestFrames.createTestKeepaliveFrame;
import static infra.remoting.test.TestFrames.createTestLeaseFrame;
import static infra.remoting.test.TestFrames.createTestMetadataPushFrame;
import static infra.remoting.test.TestFrames.createTestPayloadFrame;
import static infra.remoting.test.TestFrames.createTestRequestChannelFrame;
import static infra.remoting.test.TestFrames.createTestRequestFireAndForgetFrame;
import static infra.remoting.test.TestFrames.createTestRequestNFrame;
import static infra.remoting.test.TestFrames.createTestRequestResponseFrame;
import static infra.remoting.test.TestFrames.createTestRequestStreamFrame;
import static infra.remoting.test.TestFrames.createTestSetupFrame;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class MicrometerConnectionTests {

  private final Connection delegate = mock(Connection.class, RETURNS_SMART_NULLS);

  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

  @DisplayName("constructor throws NullPointerException with null connectionType")
  @Test
  void constructorNullConnectionType() {
    assertThatNullPointerException()
            .isThrownBy(() -> new MicrometerConnection(null, delegate, meterRegistry))
            .withMessage("connectionType is required");
  }

  @DisplayName("constructor throws NullPointerException with null delegate")
  @Test
  void constructorNullDelegate() {
    assertThatNullPointerException()
            .isThrownBy(() -> new MicrometerConnection(CLIENT, null, meterRegistry))
            .withMessage("delegate is required");
  }

  @DisplayName("constructor throws NullPointerException with null meterRegistry")
  @Test
  void constructorNullMeterRegistry() {

    assertThatNullPointerException()
            .isThrownBy(() -> new MicrometerConnection(CLIENT, delegate, null))
            .withMessage("meterRegistry is required");
  }

  @DisplayName("dispose gathers metrics")
  @Test
  void dispose() {
    new MicrometerConnection(
            CLIENT, delegate, meterRegistry, Tag.of("test-key", "test-value"))
            .dispose();

    assertThat(
            meterRegistry
                    .get("infra.remoting.duplex.connection.dispose")
                    .tag("connection.type", CLIENT.name())
                    .tag("test-key", "test-value")
                    .counter()
                    .count())
            .isEqualTo(1);
  }

  @DisplayName("onClose gathers metrics")
  @Test
  void onClose() {
    when(delegate.onClose()).thenReturn(Mono.empty());

    new MicrometerConnection(
            CLIENT, delegate, meterRegistry, Tag.of("test-key", "test-value"))
            .onClose()
            .subscribe(Operators.drainSubscriber());

    assertThat(
            meterRegistry
                    .get("infra.remoting.duplex.connection.close")
                    .tag("connection.type", CLIENT.name())
                    .tag("test-key", "test-value")
                    .counter()
                    .count())
            .isEqualTo(1);
  }

  @DisplayName("receive gathers metrics")
  @Test
  void receive() {
    Flux<ByteBuf> frames =
            Flux.just(
                    createTestCancelFrame(),
                    createTestErrorFrame(),
                    createTestKeepaliveFrame(),
                    createTestLeaseFrame(),
                    createTestMetadataPushFrame(),
                    createTestPayloadFrame(),
                    createTestRequestChannelFrame(),
                    createTestRequestFireAndForgetFrame(),
                    createTestRequestNFrame(),
                    createTestRequestResponseFrame(),
                    createTestRequestStreamFrame(),
                    createTestSetupFrame());

    when(delegate.receive()).thenReturn(frames);

    new MicrometerConnection(
            CLIENT, delegate, meterRegistry, Tag.of("test-key", "test-value"))
            .receive()
            .as(StepVerifier::create)
            .expectNextCount(12)
            .verifyComplete();

    assertThat(findCounter(CLIENT, CANCEL).count()).isEqualTo(1);
    assertThat(findCounter(CLIENT, COMPLETE).count()).isEqualTo(1);
    assertThat(findCounter(CLIENT, ERROR).count()).isEqualTo(1);
    assertThat(findCounter(CLIENT, KEEPALIVE).count()).isEqualTo(1);
    assertThat(findCounter(CLIENT, LEASE).count()).isEqualTo(1);
    assertThat(findCounter(CLIENT, METADATA_PUSH).count()).isEqualTo(1);
    assertThat(findCounter(CLIENT, REQUEST_CHANNEL).count()).isEqualTo(1);
    assertThat(findCounter(CLIENT, REQUEST_FNF).count()).isEqualTo(1);
    assertThat(findCounter(CLIENT, REQUEST_N).count()).isEqualTo(1);
    assertThat(findCounter(CLIENT, REQUEST_RESPONSE).count()).isEqualTo(1);
    assertThat(findCounter(CLIENT, REQUEST_STREAM).count()).isEqualTo(1);
    assertThat(findCounter(CLIENT, SETUP).count()).isEqualTo(1);
  }

  @DisplayName("send gathers metrics")
  @SuppressWarnings("unchecked")
  @Test
  void send() {
    ArgumentCaptor<ByteBuf> captor = ArgumentCaptor.forClass(ByteBuf.class);
    doNothing().when(delegate).sendFrame(Mockito.anyInt(), captor.capture());

    final MicrometerConnection micrometerConnection =
            new MicrometerConnection(
                    SERVER, delegate, meterRegistry, Tag.of("test-key", "test-value"));
    micrometerConnection.sendFrame(1, createTestCancelFrame());
    micrometerConnection.sendFrame(1, createTestErrorFrame());
    micrometerConnection.sendFrame(1, createTestKeepaliveFrame());
    micrometerConnection.sendFrame(1, createTestLeaseFrame());
    micrometerConnection.sendFrame(1, createTestMetadataPushFrame());
    micrometerConnection.sendFrame(1, createTestPayloadFrame());
    micrometerConnection.sendFrame(1, createTestRequestChannelFrame());
    micrometerConnection.sendFrame(1, createTestRequestFireAndForgetFrame());
    micrometerConnection.sendFrame(1, createTestRequestNFrame());
    micrometerConnection.sendFrame(1, createTestRequestResponseFrame());
    micrometerConnection.sendFrame(1, createTestRequestStreamFrame());
    micrometerConnection.sendFrame(1, createTestSetupFrame());

    StepVerifier.create(Flux.fromIterable(captor.getAllValues()))
            .expectNextCount(12)
            .verifyComplete();

    assertThat(findCounter(SERVER, CANCEL).count()).isEqualTo(1);
    assertThat(findCounter(SERVER, COMPLETE).count()).isEqualTo(1);
    assertThat(findCounter(SERVER, ERROR).count()).isEqualTo(1);
    assertThat(findCounter(SERVER, KEEPALIVE).count()).isEqualTo(1);
    assertThat(findCounter(SERVER, LEASE).count()).isEqualTo(1);
    assertThat(findCounter(SERVER, METADATA_PUSH).count()).isEqualTo(1);
    assertThat(findCounter(SERVER, REQUEST_CHANNEL).count()).isEqualTo(1);
    assertThat(findCounter(SERVER, REQUEST_FNF).count()).isEqualTo(1);
    assertThat(findCounter(SERVER, REQUEST_N).count()).isEqualTo(1);
    assertThat(findCounter(SERVER, REQUEST_RESPONSE).count()).isEqualTo(1);
    assertThat(findCounter(SERVER, REQUEST_STREAM).count()).isEqualTo(1);
    assertThat(findCounter(SERVER, SETUP).count()).isEqualTo(1);
  }

  private Counter findCounter(Type connectionType, FrameType frameType) {
    return meterRegistry
            .get("infra.remoting.frame")
            .tag("connection.type", connectionType.name())
            .tag("frame.type", frameType.name())
            .tag("test-key", "test-value")
            .counter();
  }
}
