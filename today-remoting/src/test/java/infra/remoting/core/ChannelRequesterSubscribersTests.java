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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Publisher;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.CharsetUtil;
import infra.remoting.FrameAssert;
import infra.remoting.Channel;
import infra.remoting.buffer.LeaksTrackingByteBufAllocator;
import infra.remoting.frame.FrameHeaderCodec;
import infra.remoting.frame.FrameType;
import infra.remoting.frame.PayloadFrameCodec;
import infra.remoting.frame.decoder.PayloadDecoder;
import infra.remoting.internal.subscriber.AssertSubscriber;
import infra.remoting.test.util.TestDuplexConnection;
import infra.remoting.util.DefaultPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.util.RaceTestUtils;

import static infra.remoting.frame.FrameLengthCodec.FRAME_LENGTH_MASK;

class ChannelRequesterSubscribersTests {

  private static final Set<FrameType> REQUEST_TYPES =
          new HashSet<>(
                  Arrays.asList(
                          FrameType.METADATA_PUSH,
                          FrameType.REQUEST_FNF,
                          FrameType.REQUEST_RESPONSE,
                          FrameType.REQUEST_STREAM,
                          FrameType.REQUEST_CHANNEL));

  private LeaksTrackingByteBufAllocator allocator;
  private Channel channelRequester;
  private TestDuplexConnection connection;
  protected Sinks.Empty<Void> thisClosedSink;
  protected Sinks.Empty<Void> otherClosedSink;

  @AfterEach
  void tearDownAndCheckNoLeaks() {
    allocator.assertHasNoLeaks();
  }

  @BeforeEach
  void setUp() {
    allocator = LeaksTrackingByteBufAllocator.instrument(ByteBufAllocator.DEFAULT);
    connection = new TestDuplexConnection(allocator);
    this.thisClosedSink = Sinks.empty();
    this.otherClosedSink = Sinks.empty();
    channelRequester =
            new ChannelRequester(
                    connection,
                    PayloadDecoder.DEFAULT,
                    StreamIdProvider.forClient(),
                    0,
                    FRAME_LENGTH_MASK,
                    Integer.MAX_VALUE,
                    0,
                    0,
                    null,
                    __ -> null,
                    null,
                    thisClosedSink,
                    otherClosedSink.asMono().and(thisClosedSink.asMono()));
  }

  @ParameterizedTest
  @MethodSource("allInteractions")
  @SuppressWarnings({ "rawtypes", "unchecked" })
  void singleSubscriber(Function<Channel, Publisher<?>> interaction, FrameType requestType) {
    Flux<?> response = Flux.from(interaction.apply(channelRequester));

    AssertSubscriber assertSubscriberA = AssertSubscriber.create();
    AssertSubscriber assertSubscriberB = AssertSubscriber.create();

    response.subscribe(assertSubscriberA);
    response.subscribe(assertSubscriberB);

    if (requestType != FrameType.REQUEST_FNF && requestType != FrameType.METADATA_PUSH) {
      connection.addToReceivedBuffer(PayloadFrameCodec.encodeComplete(connection.alloc(), 1));
    }

    assertSubscriberA.assertTerminated();
    assertSubscriberB.assertTerminated();

    FrameAssert.assertThat(connection.pollFrame()).typeOf(requestType).hasNoLeaks();

    if (requestType == FrameType.REQUEST_CHANNEL) {
      FrameAssert.assertThat(connection.pollFrame()).typeOf(FrameType.COMPLETE).hasNoLeaks();
    }
  }

  @ParameterizedTest
  @MethodSource("allInteractions")
  void singleSubscriberInCaseOfRacing(
          Function<Channel, Publisher<?>> interaction, FrameType requestType) {
    for (int i = 1; i < 20000; i += 2) {
      Flux<?> response = Flux.from(interaction.apply(channelRequester));
      AssertSubscriber assertSubscriberA = AssertSubscriber.create();
      AssertSubscriber assertSubscriberB = AssertSubscriber.create();

      RaceTestUtils.race(
              () -> response.subscribe(assertSubscriberA), () -> response.subscribe(assertSubscriberB));

      if (requestType != FrameType.REQUEST_FNF && requestType != FrameType.METADATA_PUSH) {
        connection.addToReceivedBuffer(PayloadFrameCodec.encodeComplete(connection.alloc(), i));
      }

      assertSubscriberA.assertTerminated();
      assertSubscriberB.assertTerminated();

      Assertions.assertThat(new AssertSubscriber[] { assertSubscriberA, assertSubscriberB })
              .anySatisfy(as -> as.assertError(IllegalStateException.class));

      if (requestType == FrameType.REQUEST_CHANNEL) {
        Assertions.assertThat(connection.getSent())
                .hasSize(2)
                .first()
                .matches(bb -> REQUEST_TYPES.contains(FrameHeaderCodec.frameType(bb)))
                .matches(ByteBuf::release);
        Assertions.assertThat(connection.getSent())
                .element(1)
                .matches(bb -> FrameHeaderCodec.frameType(bb) == FrameType.COMPLETE)
                .matches(ByteBuf::release);
      }
      else {
        Assertions.assertThat(connection.getSent())
                .hasSize(1)
                .first()
                .matches(bb -> REQUEST_TYPES.contains(FrameHeaderCodec.frameType(bb)))
                .matches(ByteBuf::release);
      }
      connection.clearSendReceiveBuffers();
    }
  }

  @ParameterizedTest
  @MethodSource("allInteractions")
  void singleSubscriberInteractionsAreLazy(Function<Channel, Publisher<?>> interaction) {
    Flux<?> response = Flux.from(interaction.apply(channelRequester));

    Assertions.assertThat(connection.getSent().size()).isEqualTo(0);
  }

  static long requestFramesCount(Collection<ByteBuf> frames) {
    return frames
            .stream()
            .filter(frame -> REQUEST_TYPES.contains(FrameHeaderCodec.frameType(frame)))
            .count();
  }

  static Stream<Arguments> allInteractions() {
    return Stream.of(
            Arguments.of(
                    (Function<Channel, Publisher<?>>)
                            channel -> channel.fireAndForget(DefaultPayload.create("test")),
                    FrameType.REQUEST_FNF),
            Arguments.of(
                    (Function<Channel, Publisher<?>>)
                            channel -> channel.requestResponse(DefaultPayload.create("test")),
                    FrameType.REQUEST_RESPONSE),
            Arguments.of(
                    (Function<Channel, Publisher<?>>)
                            channel -> channel.requestStream(DefaultPayload.create("test")),
                    FrameType.REQUEST_STREAM),
            Arguments.of(
                    (Function<Channel, Publisher<?>>)
                            channel -> channel.requestChannel(Mono.just(DefaultPayload.create("test"))),
                    FrameType.REQUEST_CHANNEL),
            Arguments.of(
                    (Function<Channel, Publisher<?>>)
                            channel ->
                                    channel.metadataPush(
                                            DefaultPayload.create(new byte[0], "test".getBytes(CharsetUtil.UTF_8))),
                    FrameType.METADATA_PUSH));
  }
}
