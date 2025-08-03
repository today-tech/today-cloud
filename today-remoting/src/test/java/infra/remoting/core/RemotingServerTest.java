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
package infra.remoting.core;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Random;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import infra.remoting.Closeable;
import infra.remoting.FrameAssert;
import infra.remoting.Channel;
import infra.remoting.exceptions.RejectedSetupException;
import infra.remoting.frame.FrameType;
import infra.remoting.frame.KeepAliveFrameCodec;
import infra.remoting.frame.RequestResponseFrameCodec;
import infra.remoting.frame.SetupFrameCodec;
import infra.remoting.test.util.TestDuplexConnection;
import infra.remoting.test.util.TestServerTransport;
import infra.remoting.util.EmptyPayload;
import reactor.core.Scannable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import reactor.test.scheduler.VirtualTimeScheduler;

import static infra.remoting.frame.FrameLengthCodec.FRAME_LENGTH_MASK;
import static org.assertj.core.api.Assertions.assertThat;

public class RemotingServerTest {

  @Test
  public void unexpectedFramesBeforeSetupFrame() {
    TestServerTransport transport = new TestServerTransport();
    RemotingServer.create().bind(transport).block();

    final TestDuplexConnection duplexConnection = transport.connect();

    duplexConnection.addToReceivedBuffer(
            KeepAliveFrameCodec.encode(duplexConnection.alloc(), false, 1, Unpooled.EMPTY_BUFFER));

    StepVerifier.create(duplexConnection.onClose())
            .expectSubscription()
            .expectComplete()
            .verify(Duration.ofSeconds(10));

    FrameAssert.assertThat(duplexConnection.pollFrame())
            .isNotNull()
            .typeOf(FrameType.ERROR)
            .hasData("SETUP or RESUME frame must be received before any others")
            .hasStreamIdZero()
            .hasNoLeaks();
    duplexConnection.alloc().assertHasNoLeaks();
  }

  @Test
  public void timeoutOnNoFirstFrame() {
    final VirtualTimeScheduler scheduler = VirtualTimeScheduler.getOrSet();
    TestServerTransport transport = new TestServerTransport();
    try {
      RemotingServer.create().maxTimeToFirstFrame(Duration.ofMinutes(2)).bind(transport).block();

      final TestDuplexConnection duplexConnection = transport.connect();

      scheduler.advanceTimeBy(Duration.ofMinutes(1));

      Assertions.assertThat(duplexConnection.isDisposed()).isFalse();

      scheduler.advanceTimeBy(Duration.ofMinutes(1));

      StepVerifier.create(duplexConnection.onClose())
              .expectSubscription()
              .expectComplete()
              .verify(Duration.ofSeconds(10));

      FrameAssert.assertThat(duplexConnection.pollFrame()).isNull();
    }
    finally {
      transport.alloc().assertHasNoLeaks();
      VirtualTimeScheduler.reset();
    }
  }

  @Test
  public void ensuresMaxFrameLengthCanNotBeLessThenMtu() {
    RemotingServer.create()
            .fragment(128)
            .bind(new TestServerTransport().withMaxFrameLength(64))
            .as(StepVerifier::create)
            .expectErrorMessage(
                    "Configured maximumTransmissionUnit[128] exceeds configured maxFrameLength[64]")
            .verify();
  }

  @Test
  public void ensuresMaxFrameLengthCanNotBeGreaterThenMaxPayloadSize() {
    RemotingServer.create()
            .maxInboundPayloadSize(128)
            .bind(new TestServerTransport().withMaxFrameLength(256))
            .as(StepVerifier::create)
            .expectErrorMessage("Configured maxFrameLength[256] exceeds maxPayloadSize[128]")
            .verify();
  }

  @Test
  public void ensuresMaxFrameLengthCanNotBeGreaterThenMaxPossibleFrameLength() {
    RemotingServer.create()
            .bind(new TestServerTransport().withMaxFrameLength(Integer.MAX_VALUE))
            .as(StepVerifier::create)
            .expectErrorMessage(
                    "Configured maxFrameLength["
                            + Integer.MAX_VALUE
                            + "] "
                            + "exceeds maxFrameLength limit "
                            + FRAME_LENGTH_MASK)
            .verify();
  }

  @Test
  public void unexpectedFramesBeforeSetup() {
    Sinks.Empty<Void> connectedSink = Sinks.empty();

    TestServerTransport transport = new TestServerTransport();
    Closeable server =
            RemotingServer.create()
                    .acceptor(
                            (setup, sendingSocket) -> {
                              connectedSink.tryEmitEmpty();
                              return Mono.just(new Channel() { });
                            })
                    .bind(transport)
                    .block();

    byte[] bytes = new byte[16_000_000];
    new Random().nextBytes(bytes);

    TestDuplexConnection connection = transport.connect();
    connection.addToReceivedBuffer(
            RequestResponseFrameCodec.encode(
                    ByteBufAllocator.DEFAULT,
                    1,
                    false,
                    Unpooled.EMPTY_BUFFER,
                    ByteBufAllocator.DEFAULT.buffer(bytes.length).writeBytes(bytes)));

    StepVerifier.create(connection.onClose()).expectComplete().verify(Duration.ofSeconds(30));
    assertThat(connectedSink.scan(Scannable.Attr.TERMINATED))
            .as("Connection should not succeed")
            .isFalse();
    FrameAssert.assertThat(connection.pollFrame())
            .hasStreamIdZero()
            .hasData("SETUP or RESUME frame must be received before any others")
            .hasNoLeaks();
    server.dispose();
    transport.alloc().assertHasNoLeaks();
  }

  @Test
  public void ensuresErrorFrameDeliveredPriorConnectionDisposal() {
    TestServerTransport transport = new TestServerTransport();
    Closeable server = RemotingServer.create()
            .acceptor((setup, sendingSocket) -> Mono.error(new RejectedSetupException("ACCESS_DENIED")))
            .bind(transport)
            .block();

    TestDuplexConnection connection = transport.connect();
    connection.addToReceivedBuffer(
            SetupFrameCodec.encode(
                    ByteBufAllocator.DEFAULT,
                    false,
                    0,
                    1,
                    Unpooled.EMPTY_BUFFER,
                    "metadata_type",
                    "data_type",
                    EmptyPayload.INSTANCE));

    StepVerifier.create(connection.onClose()).expectComplete().verify(Duration.ofSeconds(30));
    FrameAssert.assertThat(connection.pollFrame())
            .hasStreamIdZero()
            .hasData("ACCESS_DENIED")
            .hasNoLeaks();
    server.dispose();
    transport.alloc().assertHasNoLeaks();
  }
}
