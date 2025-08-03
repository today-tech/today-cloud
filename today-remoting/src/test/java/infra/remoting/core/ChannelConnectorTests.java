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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCounted;
import infra.remoting.ConnectionSetupPayload;
import infra.remoting.FrameAssert;
import infra.remoting.Payload;
import infra.remoting.Channel;
import infra.remoting.frame.FrameType;
import infra.remoting.frame.KeepAliveFrameCodec;
import infra.remoting.frame.RequestResponseFrameCodec;
import infra.remoting.test.util.TestClientTransport;
import infra.remoting.test.util.TestDuplexConnection;
import infra.remoting.util.ByteBufPayload;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.retry.Retry;

import static infra.remoting.frame.FrameLengthCodec.FRAME_LENGTH_MASK;
import static org.assertj.core.api.Assertions.assertThat;

public class ChannelConnectorTests {

  @ParameterizedTest
  @ValueSource(strings = { "KEEPALIVE", "REQUEST_RESPONSE" })
  public void unexpectedFramesBeforeResumeOKFrame(String frameType) {
    TestClientTransport transport = new TestClientTransport();
    ChannelConnector.create()
            .resume(new Resume().retry(Retry.indefinitely()))
            .connect(transport)
            .block();

    final TestDuplexConnection duplexConnection = transport.testConnection();

    duplexConnection.addToReceivedBuffer(
            KeepAliveFrameCodec.encode(duplexConnection.alloc(), false, 1, Unpooled.EMPTY_BUFFER));
    FrameAssert.assertThat(duplexConnection.pollFrame())
            .typeOf(FrameType.SETUP)
            .hasStreamIdZero()
            .hasNoLeaks();

    FrameAssert.assertThat(duplexConnection.pollFrame()).isNull();

    duplexConnection.dispose();

    final TestDuplexConnection duplexConnection2 = transport.testConnection();

    final ByteBuf frame;
    switch (frameType) {
      case "KEEPALIVE":
        frame =
                KeepAliveFrameCodec.encode(duplexConnection2.alloc(), false, 1, Unpooled.EMPTY_BUFFER);
        break;
      case "REQUEST_RESPONSE":
      default:
        frame =
                RequestResponseFrameCodec.encode(
                        duplexConnection2.alloc(), 2, false, Unpooled.EMPTY_BUFFER, Unpooled.EMPTY_BUFFER);
    }
    duplexConnection2.addToReceivedBuffer(frame);

    StepVerifier.create(duplexConnection2.onClose())
            .expectSubscription()
            .expectComplete()
            .verify(Duration.ofSeconds(10));

    FrameAssert.assertThat(duplexConnection2.pollFrame())
            .typeOf(FrameType.RESUME)
            .hasStreamIdZero()
            .hasNoLeaks();

    FrameAssert.assertThat(duplexConnection2.pollFrame())
            .isNotNull()
            .typeOf(FrameType.ERROR)
            .hasData("RESUME_OK frame must be received before any others")
            .hasStreamIdZero()
            .hasNoLeaks();

    transport.alloc().assertHasNoLeaks();
  }

  @Test
  public void ensuresThatSetupPayloadCanBeRetained() {
    AtomicReference<ConnectionSetupPayload> retainedSetupPayload = new AtomicReference<>();
    TestClientTransport transport = new TestClientTransport();

    ByteBuf data = transport.alloc().buffer();

    data.writeCharSequence("data", CharsetUtil.UTF_8);

    ChannelConnector.create()
            .setupPayload(ByteBufPayload.create(data))
            .acceptor((setup, sendingSocket) -> {
              retainedSetupPayload.set(setup.retain());
              return Mono.just(new Channel() { });
            })
            .connect(transport)
            .block();

    assertThat(transport.testConnection().getSent())
            .hasSize(1)
            .first()
            .matches(bb -> {
              DefaultConnectionSetupPayload payload = new DefaultConnectionSetupPayload(bb);
              return !payload.hasMetadata() && payload.getDataUtf8().equals("data");
            })
            .matches(buf -> buf.refCnt() == 2)
            .matches(
                    buf -> {
                      buf.release();
                      return buf.refCnt() == 1;
                    });

    ConnectionSetupPayload setup = retainedSetupPayload.get();
    String dataUtf8 = setup.getDataUtf8();
    assertThat("data".equals(dataUtf8) && setup.release()).isTrue();
    assertThat(setup.refCnt()).isZero();

    transport.alloc().assertHasNoLeaks();
  }

  @Test
  public void ensuresThatMonoFromChannelConnectorCanBeUsedForMultipleSubscriptions() {
    Payload setupPayload = ByteBufPayload.create("TestData", "TestMetadata");
    assertThat(setupPayload.refCnt()).isOne();

    // Keep the data and metadata around so we can try changing them independently
    ByteBuf dataBuf = setupPayload.data();
    ByteBuf metadataBuf = setupPayload.metadata();
    dataBuf.retain();
    metadataBuf.retain();

    TestClientTransport testClientTransport = new TestClientTransport();
    Mono<Channel> connectionMono =
            ChannelConnector.create().setupPayload(setupPayload).connect(testClientTransport);

    connectionMono
            .as(StepVerifier::create)
            .expectNextCount(1)
            .expectComplete()
            .verify(Duration.ofMillis(100));

    assertThat(testClientTransport.testConnection().getSent())
            .hasSize(1)
            .allMatch(
                    bb -> {
                      DefaultConnectionSetupPayload payload = new DefaultConnectionSetupPayload(bb);
                      return payload.getDataUtf8().equals("TestData")
                              && payload.getMetadataUtf8().equals("TestMetadata");
                    })
            .allMatch(ReferenceCounted::release);

    connectionMono
            .as(StepVerifier::create)
            .expectNextCount(1)
            .expectComplete()
            .verify(Duration.ofMillis(100));

    // Changing the original data and metadata should not impact the SetupPayload
    dataBuf.writerIndex(dataBuf.readerIndex());
    dataBuf.writeChar('d');
    dataBuf.release();

    metadataBuf.writerIndex(metadataBuf.readerIndex());
    metadataBuf.writeChar('m');
    metadataBuf.release();

    assertThat(testClientTransport.testConnection().getSent())
            .hasSize(1)
            .allMatch(
                    bb -> {
                      DefaultConnectionSetupPayload payload = new DefaultConnectionSetupPayload(bb);
                      return payload.getDataUtf8().equals("TestData")
                              && payload.getMetadataUtf8().equals("TestMetadata");
                    })
            .allMatch(
                    byteBuf -> {
                      System.out.println("calling release " + byteBuf.refCnt());
                      return byteBuf.release();
                    });
    assertThat(setupPayload.refCnt()).isZero();

    testClientTransport.alloc().assertHasNoLeaks();
  }

  @Test
  public void ensuresThatSetupPayloadProvidedAsMonoIsReleased() {
    List<Payload> saved = new ArrayList<>();
    AtomicLong subscriptions = new AtomicLong();
    Mono<Payload> setupPayloadMono =
            Mono.create(
                    sink -> {
                      final long subscriptionN = subscriptions.getAndIncrement();
                      Payload payload =
                              ByteBufPayload.create("TestData" + subscriptionN, "TestMetadata" + subscriptionN);
                      saved.add(payload);
                      sink.success(payload);
                    });

    TestClientTransport testClientTransport = new TestClientTransport();
    Mono<Channel> connectionMono =
            ChannelConnector.create().setupPayload(setupPayloadMono).connect(testClientTransport);

    connectionMono
            .as(StepVerifier::create)
            .expectNextCount(1)
            .expectComplete()
            .verify(Duration.ofMillis(100));

    assertThat(testClientTransport.testConnection().getSent())
            .hasSize(1)
            .allMatch(
                    bb -> {
                      DefaultConnectionSetupPayload payload = new DefaultConnectionSetupPayload(bb);
                      return payload.getDataUtf8().equals("TestData0")
                              && payload.getMetadataUtf8().equals("TestMetadata0");
                    })
            .allMatch(ReferenceCounted::release);

    connectionMono
            .as(StepVerifier::create)
            .expectNextCount(1)
            .expectComplete()
            .verify(Duration.ofMillis(100));

    assertThat(testClientTransport.testConnection().getSent())
            .hasSize(1)
            .allMatch(
                    bb -> {
                      DefaultConnectionSetupPayload payload = new DefaultConnectionSetupPayload(bb);
                      return payload.getDataUtf8().equals("TestData1")
                              && payload.getMetadataUtf8().equals("TestMetadata1");
                    })
            .allMatch(ReferenceCounted::release);

    assertThat(saved)
            .as("Metadata and data were consumed and released as slices")
            .allMatch(
                    payload ->
                            payload.refCnt() == 1
                                    && payload.data().refCnt() == 0
                                    && payload.metadata().refCnt() == 0);

    testClientTransport.alloc().assertHasNoLeaks();
  }

  @Test
  public void ensuresMaxFrameLengthCanNotBeLessThenMtu() {
    ChannelConnector.create()
            .fragment(128)
            .connect(new TestClientTransport().withMaxFrameLength(64))
            .as(StepVerifier::create)
            .expectErrorMessage(
                    "Configured maximumTransmissionUnit[128] exceeds configured maxFrameLength[64]")
            .verify();
  }

  @Test
  public void ensuresMaxFrameLengthCanNotBeGreaterThenMaxPayloadSize() {
    ChannelConnector.create()
            .maxInboundPayloadSize(128)
            .connect(new TestClientTransport().withMaxFrameLength(256))
            .as(StepVerifier::create)
            .expectErrorMessage("Configured maxFrameLength[256] exceeds maxPayloadSize[128]")
            .verify();
  }

  @Test
  public void ensuresMaxFrameLengthCanNotBeGreaterThenMaxPossibleFrameLength() {
    ChannelConnector.create()
            .connect(new TestClientTransport().withMaxFrameLength(Integer.MAX_VALUE))
            .as(StepVerifier::create)
            .expectErrorMessage(
                    "Configured maxFrameLength["
                            + Integer.MAX_VALUE
                            + "] "
                            + "exceeds maxFrameLength limit "
                            + FRAME_LENGTH_MASK)
            .verify();
  }
}
