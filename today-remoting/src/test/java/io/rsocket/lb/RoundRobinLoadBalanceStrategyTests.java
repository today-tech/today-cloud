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
import io.rsocket.util.EmptyPayload;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.test.publisher.TestPublisher;

public class RoundRobinLoadBalanceStrategyTests {

  @BeforeEach
  void setUp() {
    Hooks.onErrorDropped((__) -> { });
  }

  @AfterAll
  static void afterAll() {
    Hooks.resetOnErrorDropped();
  }

  @Test
  public void shouldDeliverValuesProportionally() {
    final AtomicInteger counter1 = new AtomicInteger();
    final AtomicInteger counter2 = new AtomicInteger();
    final ClientTransport mockTransport = Mockito.mock(ClientTransport.class);
    final ChannelConnector channelConnectorMock = Mockito.mock(ChannelConnector.class);

    Mockito.when(channelConnectorMock.connect(Mockito.any(ClientTransport.class)))
            .then(
                    im ->
                            Mono.just(
                                    new LoadbalanceTests.TestChannel(
                                            new Channel() {
                                              @Override
                                              public Mono<Void> fireAndForget(Payload payload) {
                                                counter1.incrementAndGet();
                                                return Mono.empty();
                                              }
                                            })))
            .then(
                    im ->
                            Mono.just(
                                    new LoadbalanceTests.TestChannel(
                                            new Channel() {
                                              @Override
                                              public Mono<Void> fireAndForget(Payload payload) {
                                                counter2.incrementAndGet();
                                                return Mono.empty();
                                              }
                                            })));

    final TestPublisher<List<LoadBalanceTarget>> source = TestPublisher.create();
    final ChannelPool channelPool =
            new ChannelPool(channelConnectorMock, source, new RoundRobinLoadBalanceStrategy());

    for (int j = 0; j < RaceTestConstants.REPEATS; j++) {
      channelPool.select().fireAndForget(EmptyPayload.INSTANCE).subscribe();
    }

    source.next(
            Arrays.asList(
                    LoadBalanceTarget.of("1", mockTransport),
                    LoadBalanceTarget.of("2", mockTransport)));

    Assertions.assertThat(counter1.get()).isCloseTo(500, Offset.offset(1));
    Assertions.assertThat(counter2.get()).isCloseTo(500, Offset.offset(1));
  }

  @Test
  public void shouldDeliverValuesToNewlyConnectedSockets() {
    final AtomicInteger counter1 = new AtomicInteger();
    final AtomicInteger counter2 = new AtomicInteger();
    final ClientTransport mockTransport1 = Mockito.mock(ClientTransport.class);
    final ClientTransport mockTransport2 = Mockito.mock(ClientTransport.class);
    final ChannelConnector channelConnectorMock = Mockito.mock(ChannelConnector.class);

    Mockito.when(channelConnectorMock.connect(Mockito.any(ClientTransport.class)))
            .then(im -> Mono.just(new LoadbalanceTests.TestChannel(
                    new Channel() {
                      @Override
                      public Mono<Void> fireAndForget(Payload payload) {
                        if (im.getArgument(0) == mockTransport1) {
                          counter1.incrementAndGet();
                        }
                        else {
                          counter2.incrementAndGet();
                        }
                        return Mono.empty();
                      }
                    })));

    final TestPublisher<List<LoadBalanceTarget>> source = TestPublisher.create();
    final ChannelPool channelPool =
            new ChannelPool(channelConnectorMock, source, new RoundRobinLoadBalanceStrategy());

    for (int j = 0; j < RaceTestConstants.REPEATS; j++) {
      channelPool.select().fireAndForget(EmptyPayload.INSTANCE).subscribe();
    }

    source.next(Collections.singletonList(LoadBalanceTarget.of("1", mockTransport1)));

    Assertions.assertThat(counter1.get()).isCloseTo(RaceTestConstants.REPEATS, Offset.offset(1));

    source.next(Collections.emptyList());

    for (int j = 0; j < RaceTestConstants.REPEATS; j++) {
      channelPool.select().fireAndForget(EmptyPayload.INSTANCE).subscribe();
    }

    source.next(Collections.singletonList(LoadBalanceTarget.of("1", mockTransport1)));

    Assertions.assertThat(counter1.get())
            .isCloseTo(RaceTestConstants.REPEATS * 2, Offset.offset(1));

    source.next(Arrays.asList(
            LoadBalanceTarget.of("1", mockTransport1),
            LoadBalanceTarget.of("2", mockTransport2)));

    for (int j = 0; j < RaceTestConstants.REPEATS; j++) {
      channelPool.select().fireAndForget(EmptyPayload.INSTANCE).subscribe();
    }

    Assertions.assertThat(counter1.get())
            .isCloseTo(RaceTestConstants.REPEATS * 2 + RaceTestConstants.REPEATS / 2, Offset.offset(1));
    Assertions.assertThat(counter2.get())
            .isCloseTo(RaceTestConstants.REPEATS / 2, Offset.offset(1));

    source.next(Collections.singletonList(LoadBalanceTarget.of("2", mockTransport1)));

    for (int j = 0; j < RaceTestConstants.REPEATS; j++) {
      channelPool.select().fireAndForget(EmptyPayload.INSTANCE).subscribe();
    }

    Assertions.assertThat(counter1.get())
            .isCloseTo(RaceTestConstants.REPEATS * 2 + RaceTestConstants.REPEATS / 2, Offset.offset(1));
    Assertions.assertThat(counter2.get())
            .isCloseTo(RaceTestConstants.REPEATS + RaceTestConstants.REPEATS / 2, Offset.offset(1));

    source.next(
            Arrays.asList(
                    LoadBalanceTarget.of("1", mockTransport1),
                    LoadBalanceTarget.of("2", mockTransport2)));

    for (int j = 0; j < RaceTestConstants.REPEATS; j++) {
      channelPool.select().fireAndForget(EmptyPayload.INSTANCE).subscribe();
    }

    Assertions.assertThat(counter1.get())
            .isCloseTo(RaceTestConstants.REPEATS * 3, Offset.offset(1));
    Assertions.assertThat(counter2.get())
            .isCloseTo(RaceTestConstants.REPEATS * 2, Offset.offset(1));
  }
}
