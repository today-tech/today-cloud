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

package io.rsocket.lb;

import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.rsocket.Payload;
import io.rsocket.Channel;
import io.rsocket.RaceTestConstants;
import io.rsocket.core.ChannelConnector;
import io.rsocket.transport.ClientTransport;
import io.rsocket.util.Clock;
import io.rsocket.util.EmptyPayload;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.publisher.TestPublisher;

public class WeightedLoadBalanceStrategyTests {

  @BeforeEach
  void setUp() {
    Hooks.onErrorDropped((__) -> { });
  }

  @AfterAll
  static void afterAll() {
    Hooks.resetOnErrorDropped();
  }

  @Test
  public void allRequestsShouldGoToTheSocketWithHigherWeight() {
    final AtomicInteger counter1 = new AtomicInteger();
    final AtomicInteger counter2 = new AtomicInteger();
    final ClientTransport mockTransport = Mockito.mock(ClientTransport.class);
    final ChannelConnector channelConnectorMock = Mockito.mock(ChannelConnector.class);
    final WeightedTestChannel rSocket1 =
            new WeightedTestChannel(
                    new Channel() {
                      @Override
                      public Mono<Void> fireAndForget(Payload payload) {
                        counter1.incrementAndGet();
                        return Mono.empty();
                      }
                    });
    final WeightedTestChannel rSocket2 =
            new WeightedTestChannel(
                    new Channel() {
                      @Override
                      public Mono<Void> fireAndForget(Payload payload) {
                        counter2.incrementAndGet();
                        return Mono.empty();
                      }
                    });
    Mockito.when(channelConnectorMock.connect(Mockito.any(ClientTransport.class)))
            .then(im -> Mono.just(rSocket1))
            .then(im -> Mono.just(rSocket2));

    final TestPublisher<List<LoadBalanceTarget>> source = TestPublisher.create();
    final ChannelPool channelPool =
            new ChannelPool(
                    channelConnectorMock,
                    source,
                    WeightedLoadBalanceStrategy.builder()
                            .weightedStatsResolver(r -> r instanceof WeightedStats ? (WeightedStats) r : null)
                            .build());

    for (int j = 0; j < RaceTestConstants.REPEATS; j++) {
      channelPool.select().fireAndForget(EmptyPayload.INSTANCE).subscribe();
    }

    source.next(
            Arrays.asList(
                    LoadBalanceTarget.of("1", mockTransport),
                    LoadBalanceTarget.of("2", mockTransport)));

    Assertions.assertThat(counter1.get())
            .describedAs("c1=" + counter1.get() + " c2=" + counter2.get())
            .isCloseTo(
                    RaceTestConstants.REPEATS, Offset.offset(Math.round(RaceTestConstants.REPEATS * 0.1f)));
    Assertions.assertThat(counter2.get())
            .describedAs("c1=" + counter1.get() + " c2=" + counter2.get())
            .isCloseTo(0, Offset.offset(Math.round(RaceTestConstants.REPEATS * 0.1f)));
  }

  @Test
  public void shouldDeliverValuesToTheSocketWithTheHighestCalculatedWeight() {
    final AtomicInteger counter1 = new AtomicInteger();
    final AtomicInteger counter2 = new AtomicInteger();
    final ClientTransport mockTransport1 = Mockito.mock(ClientTransport.class);
    final ClientTransport mockTransport2 = Mockito.mock(ClientTransport.class);
    final ChannelConnector channelConnectorMock = Mockito.mock(ChannelConnector.class);
    final WeightedTestChannel rSocket1 =
            new WeightedTestChannel(
                    new Channel() {
                      @Override
                      public Mono<Void> fireAndForget(Payload payload) {
                        counter1.incrementAndGet();
                        return Mono.empty();
                      }
                    });
    final WeightedTestChannel rSocket2 =
            new WeightedTestChannel(
                    new Channel() {
                      @Override
                      public Mono<Void> fireAndForget(Payload payload) {
                        counter1.incrementAndGet();
                        return Mono.empty();
                      }
                    });
    final WeightedTestChannel rSocket3 =
            new WeightedTestChannel(
                    new Channel() {
                      @Override
                      public Mono<Void> fireAndForget(Payload payload) {
                        counter2.incrementAndGet();
                        return Mono.empty();
                      }
                    });

    Mockito.when(channelConnectorMock.connect(Mockito.any(ClientTransport.class)))
            .then(im -> Mono.just(rSocket1))
            .then(im -> Mono.just(rSocket2))
            .then(im -> Mono.just(rSocket3));

    final TestPublisher<List<LoadBalanceTarget>> source = TestPublisher.create();
    final ChannelPool channelPool =
            new ChannelPool(
                    channelConnectorMock,
                    source,
                    WeightedLoadBalanceStrategy.builder()
                            .weightedStatsResolver(r -> r instanceof WeightedStats ? (WeightedStats) r : null)
                            .build());

    for (int j = 0; j < RaceTestConstants.REPEATS; j++) {
      channelPool.select().fireAndForget(EmptyPayload.INSTANCE).subscribe();
    }

    source.next(Collections.singletonList(LoadBalanceTarget.of("1", mockTransport1)));

    Assertions.assertThat(counter1.get()).isCloseTo(RaceTestConstants.REPEATS, Offset.offset(1));

    source.next(Collections.emptyList());

    for (int j = 0; j < RaceTestConstants.REPEATS; j++) {
      channelPool.select().fireAndForget(EmptyPayload.INSTANCE).subscribe();
    }

    rSocket1.updateAvailability(0.0);

    source.next(Collections.singletonList(LoadBalanceTarget.of("1", mockTransport1)));

    Assertions.assertThat(counter1.get())
            .isCloseTo(RaceTestConstants.REPEATS * 2, Offset.offset(1));

    source.next(
            Arrays.asList(
                    LoadBalanceTarget.of("1", mockTransport1),
                    LoadBalanceTarget.of("2", mockTransport2)));

    for (int j = 0; j < RaceTestConstants.REPEATS; j++) {
      final Channel channel = channelPool.select();
      channel.fireAndForget(EmptyPayload.INSTANCE).subscribe();
    }

    Assertions.assertThat(counter1.get())
            .isCloseTo(
                    RaceTestConstants.REPEATS * 3,
                    Offset.offset(Math.round(RaceTestConstants.REPEATS * 3 * 0.1f)));
    Assertions.assertThat(counter2.get())
            .isCloseTo(0, Offset.offset(Math.round(RaceTestConstants.REPEATS * 3 * 0.1f)));

    rSocket2.updateAvailability(0.0);

    source.next(Collections.singletonList(LoadBalanceTarget.of("2", mockTransport1)));

    for (int j = 0; j < RaceTestConstants.REPEATS; j++) {
      channelPool.select().fireAndForget(EmptyPayload.INSTANCE).subscribe();
    }

    Assertions.assertThat(counter1.get())
            .isCloseTo(
                    RaceTestConstants.REPEATS * 3,
                    Offset.offset(Math.round(RaceTestConstants.REPEATS * 4 * 0.1f)));
    Assertions.assertThat(counter2.get())
            .isCloseTo(
                    RaceTestConstants.REPEATS,
                    Offset.offset(Math.round(RaceTestConstants.REPEATS * 4 * 0.1f)));

    source.next(
            Arrays.asList(
                    LoadBalanceTarget.of("1", mockTransport1),
                    LoadBalanceTarget.of("2", mockTransport2)));

    for (int j = 0; j < RaceTestConstants.REPEATS; j++) {
      final Channel channel = channelPool.select();
      channel.fireAndForget(EmptyPayload.INSTANCE).subscribe();
    }

    Assertions.assertThat(counter1.get())
            .isCloseTo(
                    RaceTestConstants.REPEATS * 3,
                    Offset.offset(Math.round(RaceTestConstants.REPEATS * 5 * 0.1f)));
    Assertions.assertThat(counter2.get())
            .isCloseTo(
                    RaceTestConstants.REPEATS * 2,
                    Offset.offset(Math.round(RaceTestConstants.REPEATS * 5 * 0.1f)));
  }

  static class WeightedTestChannel extends BaseWeightedStats implements Channel {

    final Sinks.Empty<Void> sink = Sinks.empty();

    final Channel channel;

    public WeightedTestChannel(Channel channel) {
      this.channel = channel;
    }

    @Override
    public Mono<Void> fireAndForget(Payload payload) {
      startRequest();
      final long startTime = Clock.now();
      return this.channel
              .fireAndForget(payload)
              .doFinally(
                      __ -> {
                        stopRequest(startTime);
                        record(Clock.now() - startTime);
                        updateAvailability(1.0);
                      });
    }

    @Override
    public Mono<Void> onClose() {
      return sink.asMono();
    }

    @Override
    public void dispose() {
      sink.tryEmitEmpty();
    }

    public Channel source() {
      return channel;
    }
  }
}
