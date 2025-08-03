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
import org.mockito.Mockito;

import java.time.Duration;

import infra.remoting.Channel;
import infra.remoting.FrameAssert;
import infra.remoting.buffer.LeaksTrackingByteBufAllocator;
import infra.remoting.exceptions.ConnectionErrorException;
import infra.remoting.frame.FrameHeaderCodec;
import infra.remoting.frame.FrameType;
import infra.remoting.frame.KeepAliveFrameCodec;
import infra.remoting.frame.decoder.PayloadDecoder;
import infra.remoting.resume.ChannelSession;
import infra.remoting.resume.InMemoryResumableFramesStore;
import infra.remoting.resume.ResumableDuplexConnection;
import infra.remoting.resume.ResumeStateHolder;
import infra.remoting.test.util.TestDuplexConnection;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import reactor.test.scheduler.VirtualTimeScheduler;

import static infra.remoting.frame.FrameLengthCodec.FRAME_LENGTH_MASK;
import static infra.remoting.keepalive.KeepAliveHandler.DefaultKeepAliveHandler;
import static infra.remoting.keepalive.KeepAliveHandler.ResumableKeepAliveHandler;

public class KeepAliveTest {
  private static final int KEEP_ALIVE_INTERVAL = 100;
  private static final int KEEP_ALIVE_TIMEOUT = 1000;
  private static final int RESUMABLE_KEEP_ALIVE_TIMEOUT = 200;

  VirtualTimeScheduler virtualTimeScheduler;

  @BeforeEach
  public void setUp() {
    virtualTimeScheduler = VirtualTimeScheduler.getOrSet();
  }

  @AfterEach
  public void tearDown() {
    VirtualTimeScheduler.reset();
  }

  static ChannelState requester(int tickPeriod, int timeout) {
    LeaksTrackingByteBufAllocator allocator =
            LeaksTrackingByteBufAllocator.instrument(ByteBufAllocator.DEFAULT);
    TestDuplexConnection connection = new TestDuplexConnection(allocator);
    Sinks.Empty<Void> empty = Sinks.empty();
    RequesterChannel channel =
            new RequesterChannel(
                    connection,
                    PayloadDecoder.ZERO_COPY,
                    StreamIdProvider.forClient(),
                    0,
                    FRAME_LENGTH_MASK,
                    Integer.MAX_VALUE,
                    tickPeriod,
                    timeout,
                    new DefaultKeepAliveHandler(),
                    r -> null,
                    null,
                    empty,
                    empty.asMono());
    return new ChannelState(channel, allocator, connection, empty);
  }

  static ResumableChannelState resumableRequester(int tickPeriod, int timeout) {
    LeaksTrackingByteBufAllocator allocator =
            LeaksTrackingByteBufAllocator.instrument(ByteBufAllocator.DEFAULT);
    TestDuplexConnection connection = new TestDuplexConnection(allocator);
    ResumableDuplexConnection resumableConnection =
            new ResumableDuplexConnection(
                    "test",
                    Unpooled.EMPTY_BUFFER,
                    connection,
                    new InMemoryResumableFramesStore("test", Unpooled.EMPTY_BUFFER, 10_000));
    Sinks.Empty<Void> onClose = Sinks.empty();

    RequesterChannel channel =
            new RequesterChannel(
                    resumableConnection,
                    PayloadDecoder.ZERO_COPY,
                    StreamIdProvider.forClient(),
                    0,
                    FRAME_LENGTH_MASK,
                    Integer.MAX_VALUE,
                    tickPeriod,
                    timeout,
                    new ResumableKeepAliveHandler(
                            resumableConnection,
                            Mockito.mock(ChannelSession.class),
                            Mockito.mock(ResumeStateHolder.class)),
                    __ -> null,
                    null,
                    onClose,
                    onClose.asMono());
    return new ResumableChannelState(channel, connection, resumableConnection, onClose, allocator);
  }

  @Test
  void channelNotDisposedOnPresentKeepAlives() {
    ChannelState requesterState = requester(KEEP_ALIVE_INTERVAL, KEEP_ALIVE_TIMEOUT);

    TestDuplexConnection connection = requesterState.connection();

    Disposable disposable =
            Flux.interval(Duration.ofMillis(KEEP_ALIVE_INTERVAL))
                    .subscribe(
                            n ->
                                    connection.addToReceivedBuffer(
                                            KeepAliveFrameCodec.encode(
                                                    requesterState.allocator, true, 0, Unpooled.EMPTY_BUFFER)));

    virtualTimeScheduler.advanceTimeBy(Duration.ofMillis(KEEP_ALIVE_TIMEOUT * 2));

    Channel channel = requesterState.channel();

    Assertions.assertThat(channel.isDisposed()).isFalse();

    disposable.dispose();

    requesterState.connection.dispose();
    requesterState.channel.dispose();

    Assertions.assertThat(requesterState.connection.getSent()).allMatch(ByteBuf::release);

    requesterState.allocator.assertHasNoLeaks();
  }

  @Test
  void noKeepAlivesSentAfterChannelDispose() {
    ChannelState requesterState = requester(KEEP_ALIVE_INTERVAL, KEEP_ALIVE_TIMEOUT);

    requesterState.channel().dispose();

    Duration duration = Duration.ofMillis(500);

    virtualTimeScheduler.advanceTimeBy(duration);

    FrameAssert.assertThat(requesterState.connection.pollFrame())
            .typeOf(FrameType.ERROR)
            .hasData("Disposed")
            .hasNoLeaks();
    FrameAssert.assertThat(requesterState.connection.pollFrame()).isNull();
    requesterState.allocator.assertHasNoLeaks();
  }

  @Test
  void channelDisposedOnMissingKeepAlives() {
    ChannelState requesterState = requester(KEEP_ALIVE_INTERVAL, KEEP_ALIVE_TIMEOUT);

    Channel channel = requesterState.channel();

    virtualTimeScheduler.advanceTimeBy(Duration.ofMillis(KEEP_ALIVE_TIMEOUT * 2));

    Assertions.assertThat(channel.isDisposed()).isTrue();
    channel
            .onClose()
            .as(StepVerifier::create)
            .expectError(ConnectionErrorException.class)
            .verify(Duration.ofMillis(100));

    Assertions.assertThat(requesterState.connection.getSent()).allMatch(ByteBuf::release);

    requesterState.allocator.assertHasNoLeaks();
  }

  @Test
  void clientRequesterSendsKeepAlives() {
    ChannelState channelState = requester(KEEP_ALIVE_INTERVAL, KEEP_ALIVE_TIMEOUT);
    TestDuplexConnection connection = channelState.connection();

    virtualTimeScheduler.advanceTimeBy(Duration.ofMillis(KEEP_ALIVE_INTERVAL));
    this.keepAliveFrameWithRespondFlag(connection.pollFrame());
    virtualTimeScheduler.advanceTimeBy(Duration.ofMillis(KEEP_ALIVE_INTERVAL));
    this.keepAliveFrameWithRespondFlag(connection.pollFrame());
    virtualTimeScheduler.advanceTimeBy(Duration.ofMillis(KEEP_ALIVE_INTERVAL));
    this.keepAliveFrameWithRespondFlag(connection.pollFrame());

    channelState.channel.dispose();
    FrameAssert.assertThat(connection.pollFrame())
            .typeOf(FrameType.ERROR)
            .hasData("Disposed")
            .hasNoLeaks();
    channelState.connection.dispose();

    channelState.allocator.assertHasNoLeaks();
  }

  @Test
  void requesterRespondsToKeepAlives() {
    ChannelState channelState = requester(100_000, 100_000);
    TestDuplexConnection connection = channelState.connection();
    Duration duration = Duration.ofMillis(100);
    Mono.delay(duration)
            .subscribe(
                    l ->
                            connection.addToReceivedBuffer(
                                    KeepAliveFrameCodec.encode(
                                            channelState.allocator, true, 0, Unpooled.EMPTY_BUFFER)));

    virtualTimeScheduler.advanceTimeBy(duration);
    FrameAssert.assertThat(connection.awaitFrame())
            .typeOf(FrameType.KEEPALIVE)
            .matches(this::keepAliveFrameWithoutRespondFlag);

    channelState.channel.dispose();
    FrameAssert.assertThat(channelState.connection.pollFrame())
            .typeOf(FrameType.ERROR)
            .hasStreamIdZero()
            .hasData("Disposed")
            .hasNoLeaks();
    channelState.connection.dispose();

    channelState.allocator.assertHasNoLeaks();
  }

  @Test
  void resumableRequesterNoKeepAlivesAfterDisconnect() {
    ResumableChannelState channelState =
            resumableRequester(KEEP_ALIVE_INTERVAL, KEEP_ALIVE_TIMEOUT);
    TestDuplexConnection testConnection = channelState.connection();
    ResumableDuplexConnection resumableDuplexConnection = channelState.resumableDuplexConnection();

    resumableDuplexConnection.disconnect();

    Duration duration = Duration.ofMillis(KEEP_ALIVE_INTERVAL * 5);
    virtualTimeScheduler.advanceTimeBy(duration);
    Assertions.assertThat(testConnection.pollFrame()).isNull();

    channelState.channel.dispose();
    channelState.connection.dispose();

    channelState.allocator.assertHasNoLeaks();
  }

  @Test
  void resumableRequesterKeepAlivesAfterReconnect() {
    ResumableChannelState channelState =
            resumableRequester(KEEP_ALIVE_INTERVAL, KEEP_ALIVE_TIMEOUT);
    ResumableDuplexConnection resumableDuplexConnection = channelState.resumableDuplexConnection();
    resumableDuplexConnection.disconnect();
    TestDuplexConnection newTestConnection = new TestDuplexConnection(channelState.alloc());
    resumableDuplexConnection.connect(newTestConnection);
    //    resumableDuplexConnection.(0, 0, ignored -> Mono.empty());

    virtualTimeScheduler.advanceTimeBy(Duration.ofMillis(KEEP_ALIVE_INTERVAL));

    FrameAssert.assertThat(newTestConnection.awaitFrame())
            .typeOf(FrameType.KEEPALIVE)
            .hasStreamIdZero()
            .hasNoLeaks();

    channelState.channel.dispose();
    FrameAssert.assertThat(newTestConnection.pollFrame())
            .typeOf(FrameType.ERROR)
            .hasStreamIdZero()
            .hasData("Disposed")
            .hasNoLeaks();
    FrameAssert.assertThat(newTestConnection.pollFrame())
            .typeOf(FrameType.ERROR)
            .hasStreamIdZero()
            .hasData("Connection Closed Unexpectedly") // API limitations
            .hasNoLeaks();
    newTestConnection.dispose();

    channelState.allocator.assertHasNoLeaks();
  }

  @Test
  void resumableRequesterNoKeepAlivesAfterDispose() {
    ResumableChannelState channelState =
            resumableRequester(KEEP_ALIVE_INTERVAL, KEEP_ALIVE_TIMEOUT);
    channelState.channel().dispose();
    Duration duration = Duration.ofMillis(500);
    StepVerifier.create(Flux.from(channelState.connection().getSentAsPublisher()).take(duration))
            .then(() -> virtualTimeScheduler.advanceTimeBy(duration))
            .expectComplete()
            .verify(Duration.ofSeconds(5));

    channelState.channel.dispose();
    FrameAssert.assertThat(channelState.connection.pollFrame())
            .typeOf(FrameType.ERROR)
            .hasStreamIdZero()
            .hasData("Disposed")
            .hasNoLeaks();
    channelState.connection.dispose();
    FrameAssert.assertThat(channelState.connection.pollFrame())
            .typeOf(FrameType.ERROR)
            .hasStreamIdZero()
            .hasData("Connection Closed Unexpectedly")
            .hasNoLeaks();

    channelState.allocator.assertHasNoLeaks();
  }

  @Test
  void resumableChannelsNotDisposedOnMissingKeepAlives() throws InterruptedException {
    ResumableChannelState resumableRequesterState =
            resumableRequester(KEEP_ALIVE_INTERVAL, RESUMABLE_KEEP_ALIVE_TIMEOUT);
    Channel channel = resumableRequesterState.channel();
    TestDuplexConnection connection = resumableRequesterState.connection();

    virtualTimeScheduler.advanceTimeBy(Duration.ofMillis(500));

    Assertions.assertThat(channel.isDisposed()).isFalse();
    Assertions.assertThat(connection.isDisposed()).isTrue();

    Assertions.assertThat(resumableRequesterState.connection.getSent()).allMatch(ByteBuf::release);

    resumableRequesterState.connection.dispose();
    resumableRequesterState.channel.dispose();

    resumableRequesterState.allocator.assertHasNoLeaks();
  }

  private boolean keepAliveFrame(ByteBuf frame) {
    return FrameHeaderCodec.frameType(frame) == FrameType.KEEPALIVE;
  }

  private boolean keepAliveFrameWithRespondFlag(ByteBuf frame) {
    return keepAliveFrame(frame) && KeepAliveFrameCodec.respondFlag(frame) && frame.release();
  }

  private boolean keepAliveFrameWithoutRespondFlag(ByteBuf frame) {
    return keepAliveFrame(frame) && !KeepAliveFrameCodec.respondFlag(frame) && frame.release();
  }

  static class ChannelState {
    private final Channel channel;
    private final TestDuplexConnection connection;
    private final LeaksTrackingByteBufAllocator allocator;
    private final Sinks.Empty<Void> onClose;

    public ChannelState(Channel channel,
            LeaksTrackingByteBufAllocator allocator,
            TestDuplexConnection connection,
            Sinks.Empty<Void> onClose) {
      this.channel = channel;
      this.connection = connection;
      this.allocator = allocator;
      this.onClose = onClose;
    }

    public TestDuplexConnection connection() {
      return connection;
    }

    public Channel channel() {
      return channel;
    }

    public LeaksTrackingByteBufAllocator alloc() {
      return allocator;
    }
  }

  static class ResumableChannelState {
    private final Channel channel;
    private final TestDuplexConnection connection;
    private final ResumableDuplexConnection resumableDuplexConnection;
    private final LeaksTrackingByteBufAllocator allocator;
    private final Sinks.Empty<Void> onClose;

    public ResumableChannelState(
            Channel channel,
            TestDuplexConnection connection,
            ResumableDuplexConnection resumableDuplexConnection,
            Sinks.Empty<Void> onClose,
            LeaksTrackingByteBufAllocator allocator) {
      this.channel = channel;
      this.connection = connection;
      this.resumableDuplexConnection = resumableDuplexConnection;
      this.onClose = onClose;
      this.allocator = allocator;
    }

    public TestDuplexConnection connection() {
      return connection;
    }

    public ResumableDuplexConnection resumableDuplexConnection() {
      return resumableDuplexConnection;
    }

    public Channel channel() {
      return channel;
    }

    public LeaksTrackingByteBufAllocator alloc() {
      return allocator;
    }
  }
}
