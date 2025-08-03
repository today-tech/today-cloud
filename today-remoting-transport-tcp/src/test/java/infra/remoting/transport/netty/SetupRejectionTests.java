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
package infra.remoting.transport.netty;

import org.junit.jupiter.params.provider.Arguments;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import infra.remoting.ConnectionSetupPayload;
import infra.remoting.Channel;
import infra.remoting.ChannelAcceptor;
import infra.remoting.core.ChannelConnector;
import infra.remoting.core.RemotingServer;
import infra.remoting.exceptions.RejectedSetupException;
import infra.remoting.transport.ClientTransport;
import infra.remoting.transport.ServerTransport;
import infra.remoting.transport.netty.client.TcpClientTransport;
import infra.remoting.transport.netty.client.WebsocketClientTransport;
import infra.remoting.transport.netty.server.CloseableChannel;
import infra.remoting.transport.netty.server.TcpServerTransport;
import infra.remoting.transport.netty.server.WebsocketServerTransport;
import infra.remoting.util.DefaultPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

public class SetupRejectionTests {

  /*
  TODO Fix this test
  @DisplayName(
      "Rejecting setup by server causes requester RSocket disposal and RejectedSetupException")
  @ParameterizedTest
  @MethodSource(value = "transports")*/
  void rejectSetupTcp(
          Function<InetSocketAddress, ServerTransport<CloseableChannel>> serverTransport,
          Function<InetSocketAddress, ClientTransport> clientTransport) {

    String errorMessage = "error";
    RejectingAcceptor acceptor = new RejectingAcceptor(errorMessage);
    Mono<Channel> serverRequester = acceptor.requesterRSocket();

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

    StepVerifier.create(serverRequester.flatMap(socket -> socket.onClose()))
            .expectComplete()
            .verify(Duration.ofSeconds(5));

    StepVerifier.create(clientRequester.requestResponse(DefaultPayload.create("test")))
            .expectErrorMatches(
                    err -> err instanceof RejectedSetupException && errorMessage.equals(err.getMessage()))
            .verify(Duration.ofSeconds(5));

    channel.dispose();
  }

  static Stream<Arguments> transports() {
    Function<InetSocketAddress, ServerTransport<CloseableChannel>> tcpServer =
            TcpServerTransport::create;
    Function<InetSocketAddress, ServerTransport<CloseableChannel>> wsServer =
            WebsocketServerTransport::create;
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
    public Mono<Channel> accept(ConnectionSetupPayload setup, Channel sendingSocket) {
      requesters.tryEmitNext(sendingSocket);
      return Mono.error(new RuntimeException(msg));
    }

    public Mono<Channel> requesterRSocket() {
      return requesters.asFlux().next();
    }
  }
}
