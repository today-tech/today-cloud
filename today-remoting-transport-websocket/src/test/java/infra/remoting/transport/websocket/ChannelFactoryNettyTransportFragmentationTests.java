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

package infra.remoting.transport.websocket;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.stream.Stream;

import infra.remoting.Channel;
import infra.remoting.ChannelAcceptor;
import infra.remoting.core.ChannelConnector;
import infra.remoting.core.RemotingServer;
import infra.remoting.transport.ServerTransport;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ChannelFactoryNettyTransportFragmentationTests {

  static Stream<? extends ServerTransport<CloseableChannel>> arguments() {
    return Stream.of(WebsocketServerTransport.create(0));
  }

  @ParameterizedTest
  @MethodSource("arguments")
  void serverSucceedsWithEnabledFragmentationOnSufficientMtu(
          ServerTransport<CloseableChannel> serverTransport) {
    Mono<CloseableChannel> server =
            RemotingServer.create(mockAcceptor())
                    .fragment(100)
                    .bind(serverTransport)
                    .doOnNext(CloseableChannel::dispose);
    StepVerifier.create(server).expectNextCount(1).expectComplete().verify(Duration.ofSeconds(5));
  }

  @ParameterizedTest
  @MethodSource("arguments")
  void serverSucceedsWithDisabledFragmentation(ServerTransport<CloseableChannel> serverTransport) {
    Mono<CloseableChannel> server =
            RemotingServer.create(mockAcceptor())
                    .bind(serverTransport)
                    .doOnNext(CloseableChannel::dispose);
    StepVerifier.create(server).expectNextCount(1).expectComplete().verify(Duration.ofSeconds(5));
  }

  @ParameterizedTest
  @MethodSource("arguments")
  void clientSucceedsWithEnabledFragmentationOnSufficientMtu(
          ServerTransport<CloseableChannel> serverTransport) {
    CloseableChannel server =
            RemotingServer.create(mockAcceptor()).fragment(100).bind(serverTransport).block();

    Mono<Channel> channel =
            ChannelConnector.create()
                    .fragment(100)
                    .connect(WebsocketClientTransport.create(server.address()))
                    .doFinally(s -> server.dispose());
    StepVerifier.create(channel).expectNextCount(1).expectComplete().verify(Duration.ofSeconds(5));
  }

  @ParameterizedTest
  @MethodSource("arguments")
  void clientSucceedsWithDisabledFragmentation(ServerTransport<CloseableChannel> serverTransport) {
    CloseableChannel server = RemotingServer.create(mockAcceptor()).bind(serverTransport).block();

    Mono<Channel> channel =
            ChannelConnector.connectWith(WebsocketClientTransport.create(server.address()))
                    .doFinally(s -> server.dispose());
    StepVerifier.create(channel).expectNextCount(1).expectComplete().verify(Duration.ofSeconds(5));
  }

  private ChannelAcceptor mockAcceptor() {
    ChannelAcceptor mock = Mockito.mock(ChannelAcceptor.class);
    Mockito.when(mock.accept(Mockito.any(), Mockito.any()))
            .thenReturn(Mono.just(Mockito.mock(Channel.class)));
    return mock;
  }
}
