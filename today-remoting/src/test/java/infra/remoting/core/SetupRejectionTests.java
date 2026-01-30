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
package infra.remoting.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import infra.remoting.Channel;
import infra.remoting.ChannelAcceptor;
import infra.remoting.Closeable;
import infra.remoting.Connection;
import infra.remoting.ConnectionSetupPayload;
import infra.remoting.Payload;
import infra.remoting.buffer.LeaksTrackingByteBufAllocator;
import infra.remoting.error.Exceptions;
import infra.remoting.error.RejectedSetupException;
import infra.remoting.frame.ErrorFrameCodec;
import infra.remoting.frame.FrameHeaderCodec;
import infra.remoting.frame.FrameType;
import infra.remoting.frame.SetupFrameCodec;
import infra.remoting.test.util.TestConnection;
import infra.remoting.transport.ConnectionAcceptor;
import infra.remoting.transport.ServerTransport;
import infra.remoting.util.DefaultPayload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
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
    TestConnection conn = new TestConnection(allocator);
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
    TestConnection conn = new TestConnection(allocator);
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
    private final TestConnection conn = new TestConnection(allocator);

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

    private final Connection conn;

    TestCloseable(ConnectionAcceptor acceptor, Connection conn) {
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
