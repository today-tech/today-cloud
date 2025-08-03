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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCounted;
import infra.remoting.Payload;
import infra.remoting.Channel;
import infra.remoting.buffer.LeaksTrackingByteBufAllocator;
import infra.remoting.exceptions.Exceptions;
import infra.remoting.exceptions.RejectedException;
import infra.remoting.frame.FrameHeaderCodec;
import infra.remoting.frame.FrameType;
import infra.remoting.frame.LeaseFrameCodec;
import infra.remoting.frame.PayloadFrameCodec;
import infra.remoting.frame.RequestChannelFrameCodec;
import infra.remoting.frame.RequestFireAndForgetFrameCodec;
import infra.remoting.frame.RequestResponseFrameCodec;
import infra.remoting.frame.RequestStreamFrameCodec;
import infra.remoting.frame.SetupFrameCodec;
import infra.remoting.frame.decoder.PayloadDecoder;
import infra.remoting.internal.subscriber.AssertSubscriber;
import infra.remoting.lease.Lease;
import infra.remoting.lease.MissingLeaseException;
import infra.remoting.plugins.InitializingInterceptorRegistry;
import infra.remoting.test.util.TestClientTransport;
import infra.remoting.test.util.TestDuplexConnection;
import infra.remoting.test.util.TestServerTransport;
import infra.remoting.util.ByteBufPayload;
import infra.remoting.util.DefaultPayload;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import static infra.remoting.frame.FrameLengthCodec.FRAME_LENGTH_MASK;
import static infra.remoting.frame.FrameType.COMPLETE;
import static infra.remoting.frame.FrameType.ERROR;
import static infra.remoting.frame.FrameType.LEASE;
import static infra.remoting.frame.FrameType.REQUEST_CHANNEL;
import static infra.remoting.frame.FrameType.REQUEST_FNF;
import static infra.remoting.frame.FrameType.SETUP;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChannelLeaseTests {
  private static final String TAG = "test";

  private Channel channelRequester;
  private ResponderLeaseTracker responderLeaseTracker;
  private LeaksTrackingByteBufAllocator byteBufAllocator;
  private TestDuplexConnection connection;
  private ChannelResponder channelResponder;
  private Channel mockChannelHandler;

  private Sinks.Many<Lease> leaseSender = Sinks.many().multicast().onBackpressureBuffer();
  private RequesterLeaseTracker requesterLeaseTracker;
  protected Sinks.Empty<Void> thisClosedSink;
  protected Sinks.Empty<Void> otherClosedSink;

  @BeforeEach
  void setUp() {
    PayloadDecoder payloadDecoder = PayloadDecoder.DEFAULT;
    byteBufAllocator = LeaksTrackingByteBufAllocator.instrument(ByteBufAllocator.DEFAULT);

    connection = new TestDuplexConnection(byteBufAllocator);
    requesterLeaseTracker = new RequesterLeaseTracker(TAG, 0);
    responderLeaseTracker = new ResponderLeaseTracker(TAG, connection, () -> leaseSender.asFlux());
    this.thisClosedSink = Sinks.empty();
    this.otherClosedSink = Sinks.empty();

    ClientServerInputMultiplexer multiplexer =
            new ClientServerInputMultiplexer(connection, new InitializingInterceptorRegistry(), true);
    channelRequester =
            new ChannelRequester(
                    multiplexer.asClientConnection(),
                    payloadDecoder,
                    StreamIdProvider.forClient(),
                    0,
                    FRAME_LENGTH_MASK,
                    Integer.MAX_VALUE,
                    0,
                    0,
                    null,
                    __ -> null,
                    requesterLeaseTracker,
                    thisClosedSink,
                    otherClosedSink.asMono().and(thisClosedSink.asMono()));

    mockChannelHandler = mock(Channel.class);
    when(mockChannelHandler.metadataPush(any()))
            .then(
                    a -> {
                      Payload payload = a.getArgument(0);
                      payload.release();
                      return Mono.empty();
                    });
    when(mockChannelHandler.fireAndForget(any()))
            .then(
                    a -> {
                      Payload payload = a.getArgument(0);
                      payload.release();
                      return Mono.empty();
                    });
    when(mockChannelHandler.requestResponse(any()))
            .then(
                    a -> {
                      Payload payload = a.getArgument(0);
                      payload.release();
                      return Mono.empty();
                    });
    when(mockChannelHandler.requestStream(any()))
            .then(
                    a -> {
                      Payload payload = a.getArgument(0);
                      payload.release();
                      return Flux.empty();
                    });
    when(mockChannelHandler.requestChannel(any()))
            .then(
                    a -> {
                      Publisher<Payload> payloadPublisher = a.getArgument(0);
                      return Flux.from(payloadPublisher)
                              .doOnNext(ReferenceCounted::release)
                              .transform(
                                      Operators.lift(
                                              (__, actual) ->
                                                      new BaseSubscriber<Payload>() {
                                                        @Override
                                                        protected void hookOnSubscribe(Subscription subscription) {
                                                          actual.onSubscribe(this);
                                                        }

                                                        @Override
                                                        protected void hookOnComplete() {
                                                          actual.onComplete();
                                                        }

                                                        @Override
                                                        protected void hookOnError(Throwable throwable) {
                                                          actual.onError(throwable);
                                                        }
                                                      }));
                    });

    channelResponder =
            new ChannelResponder(
                    multiplexer.asServerConnection(),
                    mockChannelHandler,
                    payloadDecoder,
                    responderLeaseTracker,
                    0,
                    FRAME_LENGTH_MASK,
                    Integer.MAX_VALUE,
                    __ -> null,
                    otherClosedSink);
  }

  @AfterEach
  void tearDownAndCheckForLeaks() {
    byteBufAllocator.assertHasNoLeaks();
  }

  @Test
  public void serverChannelFactoryRejectsUnsupportedLease() {
    Payload payload = DefaultPayload.create(DefaultPayload.EMPTY_BUFFER);
    ByteBuf setupFrame =
            SetupFrameCodec.encode(
                    ByteBufAllocator.DEFAULT,
                    true,
                    1000,
                    30_000,
                    "application/octet-stream",
                    "application/octet-stream",
                    payload);

    TestServerTransport transport = new TestServerTransport();
    RemotingServer.create().bind(transport).block();

    TestDuplexConnection connection = transport.connect();
    connection.addToReceivedBuffer(setupFrame);

    Collection<ByteBuf> sent = connection.getSent();
    Assertions.assertThat(sent).hasSize(1);
    ByteBuf error = sent.iterator().next();
    Assertions.assertThat(FrameHeaderCodec.frameType(error)).isEqualTo(ERROR);
    Assertions.assertThat(Exceptions.from(0, error).getMessage())
            .isEqualTo("lease is not supported");
    error.release();
    connection.dispose();
    transport.alloc().assertHasNoLeaks();
  }

  @Test
  public void clientChannelFactorySetsLeaseFlag() {
    TestClientTransport clientTransport = new TestClientTransport();
    try {
      ChannelConnector.create().lease().connect(clientTransport).block();
      Collection<ByteBuf> sent = clientTransport.testConnection().getSent();
      Assertions.assertThat(sent).hasSize(1);
      ByteBuf setup = sent.iterator().next();
      Assertions.assertThat(FrameHeaderCodec.frameType(setup)).isEqualTo(SETUP);
      Assertions.assertThat(SetupFrameCodec.honorLease(setup)).isTrue();
      setup.release();
    }
    finally {
      clientTransport.testConnection().dispose();
      clientTransport.alloc().assertHasNoLeaks();
    }
  }

  @ParameterizedTest
  @MethodSource("interactions")
  void requesterMissingLeaseRequestsAreRejected(
          BiFunction<Channel, Payload, Publisher<?>> interaction) {
    Assertions.assertThat(channelRequester.availability()).isCloseTo(0.0, offset(1e-2));
    ByteBuf buffer = byteBufAllocator.buffer();
    buffer.writeCharSequence("test", CharsetUtil.UTF_8);
    Payload payload1 = ByteBufPayload.create(buffer);
    StepVerifier.create(interaction.apply(channelRequester, payload1))
            .expectError(MissingLeaseException.class)
            .verify(Duration.ofSeconds(5));

    byteBufAllocator.assertHasNoLeaks();
  }

  @ParameterizedTest
  @MethodSource("interactions")
  void requesterPresentLeaseRequestsAreAccepted(
          BiFunction<Channel, Payload, Publisher<?>> interaction, FrameType frameType) {
    ByteBuf frame = leaseFrame(5_000, 2, Unpooled.EMPTY_BUFFER);
    requesterLeaseTracker.handleLeaseFrame(frame);

    Assertions.assertThat(channelRequester.availability()).isCloseTo(1.0, offset(1e-2));
    ByteBuf buffer = byteBufAllocator.buffer();
    buffer.writeCharSequence("test", CharsetUtil.UTF_8);
    Payload payload1 = ByteBufPayload.create(buffer);
    Flux.from(interaction.apply(channelRequester, payload1))
            .as(StepVerifier::create)
            .then(
                    () -> {
                      if (frameType != REQUEST_FNF) {
                        connection.addToReceivedBuffer(
                                PayloadFrameCodec.encodeComplete(byteBufAllocator, 1));
                      }
                    })
            .expectComplete()
            .verify(Duration.ofSeconds(5));

    if (frameType == REQUEST_CHANNEL) {
      Assertions.assertThat(connection.getSent())
              .hasSize(2)
              .first()
              .matches(bb -> FrameHeaderCodec.frameType(bb) == frameType)
              .matches(ReferenceCounted::release);
      Assertions.assertThat(connection.getSent())
              .element(1)
              .matches(bb -> FrameHeaderCodec.frameType(bb) == COMPLETE)
              .matches(ReferenceCounted::release);
    }
    else {
      Assertions.assertThat(connection.getSent())
              .hasSize(1)
              .first()
              .matches(bb -> FrameHeaderCodec.frameType(bb) == frameType)
              .matches(ReferenceCounted::release);
    }

    Assertions.assertThat(channelRequester.availability()).isCloseTo(0.5, offset(1e-2));

    Assertions.assertThat(frame.release()).isTrue();

    byteBufAllocator.assertHasNoLeaks();
  }

  @ParameterizedTest
  @MethodSource("interactions")
  @SuppressWarnings({ "rawtypes", "unchecked" })
  void requesterDepletedAllowedLeaseRequestsAreRejected(
          BiFunction<Channel, Payload, Publisher<?>> interaction, FrameType interactionType) {
    ByteBuf buffer = byteBufAllocator.buffer();
    buffer.writeCharSequence("test", CharsetUtil.UTF_8);
    Payload payload1 = ByteBufPayload.create(buffer);
    ByteBuf leaseFrame = leaseFrame(5_000, 1, Unpooled.EMPTY_BUFFER);
    requesterLeaseTracker.handleLeaseFrame(leaseFrame);

    double initialAvailability = requesterLeaseTracker.availability();
    Publisher<?> request = interaction.apply(channelRequester, payload1);

    // ensures that lease is not used until the frame is sent
    Assertions.assertThat(initialAvailability).isEqualTo(requesterLeaseTracker.availability());
    Assertions.assertThat(connection.getSent()).hasSize(0);

    AssertSubscriber assertSubscriber = AssertSubscriber.create(0);
    request.subscribe(assertSubscriber);

    // if request is FNF, then request frame is sent on subscribe
    // otherwise we need to make request(1)
    if (interactionType != REQUEST_FNF) {
      Assertions.assertThat(initialAvailability).isEqualTo(requesterLeaseTracker.availability());
      Assertions.assertThat(connection.getSent()).hasSize(0);

      assertSubscriber.request(1);
    }

    // ensures availability is changed and lease is used only up on frame sending
    Assertions.assertThat(channelRequester.availability()).isCloseTo(0.0, offset(1e-2));

    if (interactionType == REQUEST_CHANNEL) {
      Assertions.assertThat(connection.getSent())
              .hasSize(2)
              .first()
              .matches(bb -> FrameHeaderCodec.frameType(bb) == interactionType)
              .matches(ReferenceCounted::release);
      Assertions.assertThat(connection.getSent())
              .element(1)
              .matches(bb -> FrameHeaderCodec.frameType(bb) == COMPLETE)
              .matches(ReferenceCounted::release);
    }
    else {
      Assertions.assertThat(connection.getSent())
              .hasSize(1)
              .first()
              .matches(bb -> FrameHeaderCodec.frameType(bb) == interactionType)
              .matches(ReferenceCounted::release);
    }

    ByteBuf buffer2 = byteBufAllocator.buffer();
    buffer2.writeCharSequence("test", CharsetUtil.UTF_8);
    Payload payload2 = ByteBufPayload.create(buffer2);
    Flux.from(interaction.apply(channelRequester, payload2))
            .as(StepVerifier::create)
            .expectError(MissingLeaseException.class)
            .verify(Duration.ofSeconds(5));

    Assertions.assertThat(leaseFrame.release()).isTrue();

    byteBufAllocator.assertHasNoLeaks();
  }

  @ParameterizedTest
  @MethodSource("interactions")
  void requesterExpiredLeaseRequestsAreRejected(
          BiFunction<Channel, Payload, Publisher<?>> interaction) {
    ByteBuf frame = leaseFrame(50, 1, Unpooled.EMPTY_BUFFER);
    requesterLeaseTracker.handleLeaseFrame(frame);

    ByteBuf buffer = byteBufAllocator.buffer();
    buffer.writeCharSequence("test", CharsetUtil.UTF_8);
    Payload payload1 = ByteBufPayload.create(buffer);

    Flux.defer(() -> interaction.apply(channelRequester, payload1))
            .delaySubscription(Duration.ofMillis(200))
            .as(StepVerifier::create)
            .expectError(MissingLeaseException.class)
            .verify(Duration.ofSeconds(5));

    Assertions.assertThat(frame.release()).isTrue();

    byteBufAllocator.assertHasNoLeaks();
  }

  @Test
  void requesterAvailabilityRespectsTransport() {
    ByteBuf frame = leaseFrame(5_000, 1, Unpooled.EMPTY_BUFFER);
    try {

      requesterLeaseTracker.handleLeaseFrame(frame);
      double unavailable = 0.0;
      connection.setAvailability(unavailable);
      Assertions.assertThat(channelRequester.availability()).isCloseTo(unavailable, offset(1e-2));
    }
    finally {
      frame.release();
    }
  }

  @ParameterizedTest
  @MethodSource("responderInteractions")
  void responderMissingLeaseRequestsAreRejected(FrameType frameType) {
    ByteBuf buffer = byteBufAllocator.buffer();
    buffer.writeCharSequence("test", CharsetUtil.UTF_8);
    Payload payload1 = ByteBufPayload.create(buffer);

    switch (frameType) {
      case REQUEST_FNF:
        final ByteBuf fnfFrame =
                RequestFireAndForgetFrameCodec.encodeReleasingPayload(byteBufAllocator, 1, payload1);
        channelResponder.handleFrame(fnfFrame);
        fnfFrame.release();
        break;
      case REQUEST_RESPONSE:
        final ByteBuf requestResponseFrame =
                RequestResponseFrameCodec.encodeReleasingPayload(byteBufAllocator, 1, payload1);
        channelResponder.handleFrame(requestResponseFrame);
        requestResponseFrame.release();
        break;
      case REQUEST_STREAM:
        final ByteBuf requestStreamFrame =
                RequestStreamFrameCodec.encodeReleasingPayload(byteBufAllocator, 1, 1, payload1);
        channelResponder.handleFrame(requestStreamFrame);
        requestStreamFrame.release();
        break;
      case REQUEST_CHANNEL:
        final ByteBuf requestChannelFrame =
                RequestChannelFrameCodec.encodeReleasingPayload(byteBufAllocator, 1, true, 1, payload1);
        channelResponder.handleFrame(requestChannelFrame);
        requestChannelFrame.release();
        break;
    }

    if (frameType != REQUEST_FNF) {
      Assertions.assertThat(connection.getSent())
              .hasSize(1)
              .first()
              .matches(bb -> FrameHeaderCodec.frameType(bb) == ERROR)
              .matches(bb -> Exceptions.from(1, bb) instanceof RejectedException)
              .matches(ReferenceCounted::release);
    }

    byteBufAllocator.assertHasNoLeaks();
  }

  @ParameterizedTest
  @MethodSource("responderInteractions")
  void responderPresentLeaseRequestsAreAccepted(FrameType frameType) {
    leaseSender.tryEmitNext(Lease.create(Duration.ofMillis(5_000), 2));

    ByteBuf buffer = byteBufAllocator.buffer();
    buffer.writeCharSequence("test", CharsetUtil.UTF_8);
    Payload payload1 = ByteBufPayload.create(buffer);

    switch (frameType) {
      case REQUEST_FNF:
        final ByteBuf fnfFrame =
                RequestFireAndForgetFrameCodec.encodeReleasingPayload(byteBufAllocator, 1, payload1);
        channelResponder.handleFireAndForget(1, fnfFrame);
        fnfFrame.release();
        break;
      case REQUEST_RESPONSE:
        final ByteBuf requestResponseFrame =
                RequestResponseFrameCodec.encodeReleasingPayload(byteBufAllocator, 1, payload1);
        channelResponder.handleFrame(requestResponseFrame);
        requestResponseFrame.release();
        break;
      case REQUEST_STREAM:
        final ByteBuf requestStreamFrame =
                RequestStreamFrameCodec.encodeReleasingPayload(byteBufAllocator, 1, 1, payload1);
        channelResponder.handleFrame(requestStreamFrame);
        requestStreamFrame.release();
        break;
      case REQUEST_CHANNEL:
        final ByteBuf requestChannelFrame =
                RequestChannelFrameCodec.encodeReleasingPayload(byteBufAllocator, 1, true, 1, payload1);
        channelResponder.handleFrame(requestChannelFrame);
        requestChannelFrame.release();
        break;
    }

    switch (frameType) {
      case REQUEST_FNF:
        Mockito.verify(mockChannelHandler).fireAndForget(any());
        break;
      case REQUEST_RESPONSE:
        Mockito.verify(mockChannelHandler).requestResponse(any());
        break;
      case REQUEST_STREAM:
        Mockito.verify(mockChannelHandler).requestStream(any());
        break;
      case REQUEST_CHANNEL:
        Mockito.verify(mockChannelHandler).requestChannel(any());
        break;
    }

    Assertions.assertThat(connection.getSent())
            .first()
            .matches(bb -> FrameHeaderCodec.frameType(bb) == LEASE)
            .matches(ReferenceCounted::release);

    if (frameType != REQUEST_FNF) {
      Assertions.assertThat(connection.getSent())
              .hasSize(2)
              .element(1)
              .matches(bb -> FrameHeaderCodec.frameType(bb) == COMPLETE)
              .matches(ReferenceCounted::release);
    }

    byteBufAllocator.assertHasNoLeaks();
  }

  @ParameterizedTest
  @MethodSource("responderInteractions")
  void responderDepletedAllowedLeaseRequestsAreRejected(FrameType frameType) {
    leaseSender.tryEmitNext(Lease.create(Duration.ofMillis(5_000), 1));

    ByteBuf buffer = byteBufAllocator.buffer();
    buffer.writeCharSequence("test", CharsetUtil.UTF_8);
    Payload payload1 = ByteBufPayload.create(buffer);

    ByteBuf buffer2 = byteBufAllocator.buffer();
    buffer2.writeCharSequence("test2", CharsetUtil.UTF_8);
    Payload payload2 = ByteBufPayload.create(buffer2);

    switch (frameType) {
      case REQUEST_FNF:
        final ByteBuf fnfFrame =
                RequestFireAndForgetFrameCodec.encodeReleasingPayload(byteBufAllocator, 1, payload1);
        final ByteBuf fnfFrame2 =
                RequestFireAndForgetFrameCodec.encodeReleasingPayload(byteBufAllocator, 3, payload2);
        channelResponder.handleFrame(fnfFrame);
        channelResponder.handleFrame(fnfFrame2);
        fnfFrame.release();
        fnfFrame2.release();
        break;
      case REQUEST_RESPONSE:
        final ByteBuf requestResponseFrame =
                RequestResponseFrameCodec.encodeReleasingPayload(byteBufAllocator, 1, payload1);
        final ByteBuf requestResponseFrame2 =
                RequestResponseFrameCodec.encodeReleasingPayload(byteBufAllocator, 3, payload2);
        channelResponder.handleFrame(requestResponseFrame);
        channelResponder.handleFrame(requestResponseFrame2);
        requestResponseFrame.release();
        requestResponseFrame2.release();
        break;
      case REQUEST_STREAM:
        final ByteBuf requestStreamFrame =
                RequestStreamFrameCodec.encodeReleasingPayload(byteBufAllocator, 1, 1, payload1);
        final ByteBuf requestStreamFrame2 =
                RequestStreamFrameCodec.encodeReleasingPayload(byteBufAllocator, 3, 1, payload2);
        channelResponder.handleFrame(requestStreamFrame);
        channelResponder.handleFrame(requestStreamFrame2);
        requestStreamFrame.release();
        requestStreamFrame2.release();
        break;
      case REQUEST_CHANNEL:
        final ByteBuf requestChannelFrame =
                RequestChannelFrameCodec.encodeReleasingPayload(byteBufAllocator, 1, true, 1, payload1);
        final ByteBuf requestChannelFrame2 =
                RequestChannelFrameCodec.encodeReleasingPayload(byteBufAllocator, 3, true, 1, payload2);
        channelResponder.handleFrame(requestChannelFrame);
        channelResponder.handleFrame(requestChannelFrame2);
        requestChannelFrame.release();
        requestChannelFrame2.release();
        break;
    }

    switch (frameType) {
      case REQUEST_FNF:
        Mockito.verify(mockChannelHandler).fireAndForget(any());
        break;
      case REQUEST_RESPONSE:
        Mockito.verify(mockChannelHandler).requestResponse(any());
        break;
      case REQUEST_STREAM:
        Mockito.verify(mockChannelHandler).requestStream(any());
        break;
      case REQUEST_CHANNEL:
        Mockito.verify(mockChannelHandler).requestChannel(any());
        break;
    }

    Assertions.assertThat(connection.getSent())
            .first()
            .matches(bb -> FrameHeaderCodec.frameType(bb) == LEASE)
            .matches(ReferenceCounted::release);

    if (frameType != REQUEST_FNF) {
      Assertions.assertThat(connection.getSent())
              .hasSize(3)
              .element(1)
              .matches(bb -> FrameHeaderCodec.frameType(bb) == COMPLETE)
              .matches(ReferenceCounted::release);

      Assertions.assertThat(connection.getSent())
              .hasSize(3)
              .element(2)
              .matches(bb -> FrameHeaderCodec.frameType(bb) == ERROR)
              .matches(bb -> Exceptions.from(1, bb) instanceof RejectedException)
              .matches(ReferenceCounted::release);
    }

    byteBufAllocator.assertHasNoLeaks();
  }

  @ParameterizedTest
  @MethodSource("interactions")
  void expiredLeaseRequestsAreRejected(BiFunction<Channel, Payload, Publisher<?>> interaction) {
    leaseSender.tryEmitNext(Lease.create(Duration.ofMillis(50), 1));

    ByteBuf buffer = byteBufAllocator.buffer();
    buffer.writeCharSequence("test", CharsetUtil.UTF_8);
    Payload payload1 = ByteBufPayload.create(buffer);

    Flux.from(interaction.apply(channelRequester, payload1))
            .delaySubscription(Duration.ofMillis(100))
            .as(StepVerifier::create)
            .expectError(MissingLeaseException.class)
            .verify(Duration.ofSeconds(5));

    Assertions.assertThat(connection.getSent())
            .hasSize(1)
            .first()
            .matches(bb -> FrameHeaderCodec.frameType(bb) == LEASE)
            .matches(ReferenceCounted::release);

    byteBufAllocator.assertHasNoLeaks();
  }

  @Test
  void sendLease() {
    ByteBuf metadata = byteBufAllocator.buffer();
    Charset utf8 = StandardCharsets.UTF_8;
    String metadataContent = "test";
    metadata.writeCharSequence(metadataContent, utf8);
    int ttl = 5_000;
    int numberOfRequests = 2;
    leaseSender.tryEmitNext(Lease.create(Duration.ofMillis(5_000), 2, metadata));

    ByteBuf leaseFrame =
            connection
                    .getSent()
                    .stream()
                    .filter(f -> FrameHeaderCodec.frameType(f) == FrameType.LEASE)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Lease frame not sent"));

    try {
      Assertions.assertThat(LeaseFrameCodec.ttl(leaseFrame)).isEqualTo(ttl);
      Assertions.assertThat(LeaseFrameCodec.numRequests(leaseFrame)).isEqualTo(numberOfRequests);
      Assertions.assertThat(LeaseFrameCodec.metadata(leaseFrame).toString(utf8))
              .isEqualTo(metadataContent);
    }
    finally {
      leaseFrame.release();
    }
  }

  //  @Test
  //  void receiveLease() {
  //    Collection<Lease> receivedLeases = new ArrayList<>();
  //    leaseReceiver.subscribe(lease -> receivedLeases.add(lease));
  //
  //    ByteBuf metadata = byteBufAllocator.buffer();
  //    Charset utf8 = StandardCharsets.UTF_8;
  //    String metadataContent = "test";
  //    metadata.writeCharSequence(metadataContent, utf8);
  //    int ttl = 5_000;
  //    int numberOfRequests = 2;
  //
  //    ByteBuf leaseFrame = leaseFrame(ttl, numberOfRequests, metadata).retain(1);
  //
  //    connection.addToReceivedBuffer(leaseFrame);
  //
  //    Assertions.assertThat(receivedLeases.isEmpty()).isFalse();
  //    Lease receivedLease = receivedLeases.iterator().next();
  //    Assertions.assertThat(receivedLease.getTimeToLiveMillis()).isEqualTo(ttl);
  //
  // Assertions.assertThat(receivedLease.getStartingAllowedRequests()).isEqualTo(numberOfRequests);
  //    Assertions.assertThat(receivedLease.metadata().toString(utf8)).isEqualTo(metadataContent);
  //
  //    ReferenceCountUtil.safeRelease(leaseFrame);
  //  }

  ByteBuf leaseFrame(int ttl, int requests, ByteBuf metadata) {
    return LeaseFrameCodec.encode(byteBufAllocator, ttl, requests, metadata);
  }

  static Stream<Arguments> interactions() {
    return Stream.of(
            Arguments.of(
                    (BiFunction<Channel, Payload, Publisher<?>>) Channel::fireAndForget,
                    FrameType.REQUEST_FNF),
            Arguments.of(
                    (BiFunction<Channel, Payload, Publisher<?>>) Channel::requestResponse,
                    FrameType.REQUEST_RESPONSE),
            Arguments.of(
                    (BiFunction<Channel, Payload, Publisher<?>>) Channel::requestStream,
                    FrameType.REQUEST_STREAM),
            Arguments.of(
                    (BiFunction<Channel, Payload, Publisher<?>>)
                            (channel, payload) -> channel.requestChannel(Mono.just(payload)),
                    FrameType.REQUEST_CHANNEL));
  }

  static Stream<FrameType> responderInteractions() {
    return Stream.of(
            FrameType.REQUEST_FNF,
            FrameType.REQUEST_RESPONSE,
            FrameType.REQUEST_STREAM,
            FrameType.REQUEST_CHANNEL);
  }
}
