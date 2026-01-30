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

import org.junit.jupiter.params.provider.Arguments;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import infra.remoting.Channel;
import infra.remoting.ChannelAcceptor;
import infra.remoting.ConnectionSetupPayload;
import infra.remoting.core.ChannelConnector;
import infra.remoting.core.RemotingServer;
import infra.remoting.error.RejectedSetupException;
import infra.remoting.transport.ClientTransport;
import infra.remoting.transport.ServerTransport;
import infra.remoting.transport.netty.client.TcpClientTransport;
import infra.remoting.transport.netty.server.TcpServerTransport;
import infra.remoting.util.DefaultPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

public class SetupRejectionTests {

  /*
  TODO Fix this test
  @DisplayName(
      "Rejecting setup by server causes requester Channel disposal and RejectedSetupException")
  @ParameterizedTest
  @MethodSource(value = "transports")*/
  void rejectSetupTcp(
          Function<InetSocketAddress, ServerTransport<CloseableChannel>> serverTransport,
          Function<InetSocketAddress, ClientTransport> clientTransport) {

    String errorMessage = "error";
    RejectingAcceptor acceptor = new RejectingAcceptor(errorMessage);
    Mono<Channel> serverRequester = acceptor.requesterChannel();

    CloseableChannel channel =
            RemotingServer.create(acceptor)
                    .bind(serverTransport.apply(new InetSocketAddress("localhost", 0)))
                    .block(Duration.ofSeconds(5));

    ErrorConsumer errorConsumer = new ErrorConsumer();

    Channel clientRequester =
            ChannelConnector.connectWith(clientTransport.apply(channel.address()))
                    .doOnError(errorConsumer)
                    .block(Duration.ofSeconds(5));

    StepVerifier.create(errorConsumer.errors().next())
            .expectNextMatches(
                    err -> err instanceof RejectedSetupException && errorMessage.equals(err.getMessage()))
            .expectComplete()
            .verify(Duration.ofSeconds(5));

    StepVerifier.create(clientRequester.onClose()).expectComplete().verify(Duration.ofSeconds(5));

    StepVerifier.create(serverRequester.flatMap(Channel::onClose))
            .expectComplete()
            .verify(Duration.ofSeconds(5));

    StepVerifier.create(clientRequester.requestResponse(DefaultPayload.create("test")))
            .expectErrorMatches(
                    err -> err instanceof RejectedSetupException && errorMessage.equals(err.getMessage()))
            .verify(Duration.ofSeconds(5));

    channel.dispose();
  }

  static Stream<Arguments> transports() {
    Function<InetSocketAddress, ServerTransport<infra.remoting.transport.netty.server.CloseableChannel>> tcpServer = TcpServerTransport::create;
    Function<InetSocketAddress, ServerTransport<CloseableChannel>> wsServer = WebsocketServerTransport::create;
    Function<InetSocketAddress, ClientTransport> tcpClient = TcpClientTransport::create;
    Function<InetSocketAddress, ClientTransport> wsClient = WebsocketClientTransport::create;

    return Stream.of(Arguments.of(tcpServer, tcpClient), Arguments.of(wsServer, wsClient));
  }

  static class ErrorConsumer implements Consumer<Throwable> {
    private final Sinks.Many<Throwable> errors = Sinks.many().multicast().onBackpressureBuffer();

    @Override
    public void accept(Throwable t) {
      errors.tryEmitNext(t);
    }

    Flux<Throwable> errors() {
      return errors.asFlux();
    }
  }

  private static class RejectingAcceptor implements ChannelAcceptor {
    private final String msg;
    private final Sinks.Many<Channel> requesters = Sinks.many().multicast().onBackpressureBuffer();

    public RejectingAcceptor(String msg) {
      this.msg = msg;
    }

    @Override
    public Mono<Channel> accept(ConnectionSetupPayload setup, Channel channel) {
      requesters.tryEmitNext(channel);
      return Mono.error(new RuntimeException(msg));
    }

    public Mono<Channel> requesterChannel() {
      return requesters.asFlux().next();
    }
  }
}
