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
