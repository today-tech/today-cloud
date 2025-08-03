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
package io.rsocket.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.Closeable;
import io.rsocket.ConnectionSetupPayload;
import io.rsocket.DuplexConnection;
import io.rsocket.Payload;
import io.rsocket.Channel;
import io.rsocket.ChannelAcceptor;
import io.rsocket.buffer.LeaksTrackingByteBufAllocator;
import io.rsocket.exceptions.Exceptions;
import io.rsocket.exceptions.RejectedSetupException;
import io.rsocket.frame.ErrorFrameCodec;
import io.rsocket.frame.FrameHeaderCodec;
import io.rsocket.frame.FrameType;
import io.rsocket.frame.SetupFrameCodec;
import io.rsocket.test.util.TestDuplexConnection;
import io.rsocket.transport.ConnectionAcceptor;
import io.rsocket.transport.ServerTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import static io.rsocket.frame.FrameLengthCodec.FRAME_LENGTH_MASK;
import static org.assertj.core.api.Assertions.assertThat;

public class SetupRejectionTest {

  @Test
  void responderRejectSetup() {
    SingleConnectionTransport transport = new SingleConnectionTransport();

    String errorMsg = "error";
    RejectingAcceptor acceptor = new RejectingAcceptor(errorMsg);
    RemotingServer.create().acceptor(acceptor).bind(transport).block();

    transport.connect();

    ByteBuf sentFrame = transport.awaitSent();
    assertThat(FrameHeaderCodec.frameType(sentFrame)).isEqualTo(FrameType.ERROR);
    RuntimeException error = Exceptions.from(0, sentFrame);
    sentFrame.release();
    assertThat(errorMsg).isEqualTo(error.getMessage());
    assertThat(error).isInstanceOf(RejectedSetupException.class);
    Channel acceptorSender = acceptor.senderRSocket().block();
    assertThat(acceptorSender.isDisposed()).isTrue();
    transport.allocator.assertHasNoLeaks();
  }

  @Test
  void requesterStreamsTerminatedOnZeroErrorFrame() {
    LeaksTrackingByteBufAllocator allocator =
            LeaksTrackingByteBufAllocator.instrument(ByteBufAllocator.DEFAULT);
    TestDuplexConnection conn = new TestDuplexConnection(allocator);
    Sinks.Empty<Void> onThisSideClosedSink = Sinks.empty();

    ChannelRequester rSocket =
            new ChannelRequester(
                    conn,
                    DefaultPayload::create,
                    StreamIdProvider.forClient(),
                    0,
                    FRAME_LENGTH_MASK,
                    Integer.MAX_VALUE,
                    0,
                    0,
                    null,
                    __ -> null,
                    null,
                    onThisSideClosedSink,
                    onThisSideClosedSink.asMono());

    String errorMsg = "error";

    StepVerifier.create(
                    rSocket
                            .requestResponse(DefaultPayload.create("test"))
                            .doOnRequest(
                                    ignored ->
                                            conn.addToReceivedBuffer(
                                                    ErrorFrameCodec.encode(
                                                            ByteBufAllocator.DEFAULT,
                                                            0,
                                                            new RejectedSetupException(errorMsg)))))
            .expectErrorMatches(
                    err -> err instanceof RejectedSetupException && errorMsg.equals(err.getMessage()))
            .verify(Duration.ofSeconds(5));

    assertThat(rSocket.isDisposed()).isTrue();
    allocator.assertHasNoLeaks();
  }

  @Test
  void requesterNewStreamsTerminatedAfterZeroErrorFrame() {
    LeaksTrackingByteBufAllocator allocator =
            LeaksTrackingByteBufAllocator.instrument(ByteBufAllocator.DEFAULT);
    TestDuplexConnection conn = new TestDuplexConnection(allocator);
    Sinks.Empty<Void> onThisSideClosedSink = Sinks.empty();
    ChannelRequester rSocket =
            new ChannelRequester(
                    conn,
                    DefaultPayload::create,
                    StreamIdProvider.forClient(),
                    0,
                    FRAME_LENGTH_MASK,
                    Integer.MAX_VALUE,
                    0,
                    0,
                    null,
                    __ -> null,
                    null,
                    onThisSideClosedSink,
                    onThisSideClosedSink.asMono());

    conn.addToReceivedBuffer(
            ErrorFrameCodec.encode(ByteBufAllocator.DEFAULT, 0, new RejectedSetupException("error")));

    StepVerifier.create(
                    rSocket
                            .requestResponse(DefaultPayload.create("test"))
                            .delaySubscription(Duration.ofMillis(100)))
            .expectErrorMatches(
                    err -> err instanceof RejectedSetupException && "error".equals(err.getMessage()))
            .verify(Duration.ofSeconds(5));
    allocator.assertHasNoLeaks();
  }

  private static class RejectingAcceptor implements ChannelAcceptor {
    private final String errorMessage;
    private final Sinks.Many<Channel> senderRSockets =
            Sinks.many().unicast().onBackpressureBuffer();

    public RejectingAcceptor(String errorMessage) {
      this.errorMessage = errorMessage;
    }

    @Override
    public Mono<Channel> accept(ConnectionSetupPayload setup, Channel sendingSocket) {
      senderRSockets.tryEmitNext(sendingSocket);
      return Mono.error(new RuntimeException(errorMessage));
    }

    public Mono<Channel> senderRSocket() {
      return senderRSockets.asFlux().next();
    }
  }

  private static class SingleConnectionTransport implements ServerTransport<TestCloseable> {

    private final LeaksTrackingByteBufAllocator allocator =
            LeaksTrackingByteBufAllocator.instrument(ByteBufAllocator.DEFAULT);
    private final TestDuplexConnection conn = new TestDuplexConnection(allocator);

    @Override
    public Mono<TestCloseable> start(ConnectionAcceptor acceptor) {
      return Mono.just(new TestCloseable(acceptor, conn));
    }

    public ByteBuf awaitSent() {
      return conn.awaitFrame();
    }

    public void connect() {
      Payload payload = DefaultPayload.create(DefaultPayload.EMPTY_BUFFER);
      ByteBuf setup = SetupFrameCodec.encode(allocator, false, 0, 42, "mdMime", "dMime", payload);

      conn.addToReceivedBuffer(setup);
    }
  }

  private static class TestCloseable implements Closeable {

    private final DuplexConnection conn;

    TestCloseable(ConnectionAcceptor acceptor, DuplexConnection conn) {
      this.conn = conn;
      Mono.from(acceptor.accept(conn)).subscribe(notUsed -> { }, err -> conn.dispose());
    }

    @Override
    public Mono<Void> onClose() {
      return conn.onClose();
    }

    @Override
    public void dispose() {
      conn.dispose();
    }
  }
}
