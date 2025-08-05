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

import java.time.Duration;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import infra.remoting.Closeable;
import infra.remoting.ConnectionSetupPayload;
import infra.remoting.DuplexConnection;
import infra.remoting.Payload;
import infra.remoting.Channel;
import infra.remoting.ChannelAcceptor;
import infra.remoting.buffer.LeaksTrackingByteBufAllocator;
import infra.remoting.error.Exceptions;
import infra.remoting.error.RejectedSetupException;
import infra.remoting.frame.ErrorFrameCodec;
import infra.remoting.frame.FrameHeaderCodec;
import infra.remoting.frame.FrameType;
import infra.remoting.frame.SetupFrameCodec;
import infra.remoting.test.util.TestDuplexConnection;
import infra.remoting.transport.ConnectionAcceptor;
import infra.remoting.transport.ServerTransport;
import infra.remoting.util.DefaultPayload;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import static infra.remoting.frame.FrameLengthCodec.FRAME_LENGTH_MASK;
import static org.assertj.core.api.Assertions.assertThat;

public class SetupRejectionTests {

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
    Channel acceptorSender = acceptor.senderChannel().block();
    assertThat(acceptorSender.isDisposed()).isTrue();
    transport.allocator.assertHasNoLeaks();
  }

  @Test
  void requesterStreamsTerminatedOnZeroErrorFrame() {
    LeaksTrackingByteBufAllocator allocator =
            LeaksTrackingByteBufAllocator.instrument(ByteBufAllocator.DEFAULT);
    TestDuplexConnection conn = new TestDuplexConnection(allocator);
    Sinks.Empty<Void> onThisSideClosedSink = Sinks.empty();

    RequesterChannel channel =
            new RequesterChannel(
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
                    channel
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

    assertThat(channel.isDisposed()).isTrue();
    allocator.assertHasNoLeaks();
  }

  @Test
  void requesterNewStreamsTerminatedAfterZeroErrorFrame() {
    LeaksTrackingByteBufAllocator allocator =
            LeaksTrackingByteBufAllocator.instrument(ByteBufAllocator.DEFAULT);
    TestDuplexConnection conn = new TestDuplexConnection(allocator);
    Sinks.Empty<Void> onThisSideClosedSink = Sinks.empty();
    RequesterChannel channel =
            new RequesterChannel(
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
                    channel
                            .requestResponse(DefaultPayload.create("test"))
                            .delaySubscription(Duration.ofMillis(100)))
            .expectErrorMatches(
                    err -> err instanceof RejectedSetupException && "error".equals(err.getMessage()))
            .verify(Duration.ofSeconds(5));
    allocator.assertHasNoLeaks();
  }

  private static class RejectingAcceptor implements ChannelAcceptor {
    private final String errorMessage;
    private final Sinks.Many<Channel> senderChannels =
            Sinks.many().unicast().onBackpressureBuffer();

    public RejectingAcceptor(String errorMessage) {
      this.errorMessage = errorMessage;
    }

    @Override
    public Mono<Channel> accept(ConnectionSetupPayload setup, Channel channel) {
      senderChannels.tryEmitNext(channel);
      return Mono.error(new RuntimeException(errorMessage));
    }

    public Mono<Channel> senderChannel() {
      return senderChannels.asFlux().next();
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
