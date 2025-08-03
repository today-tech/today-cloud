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

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

import infra.remoting.transport.netty.client.WebsocketClientTransport;
import infra.remoting.transport.netty.server.WebsocketRouteTransport;
import infra.remoting.Channel;
import infra.remoting.ChannelAcceptor;
import infra.remoting.core.ChannelConnector;
import infra.remoting.core.RemotingServer;
import infra.remoting.transport.ConnectionAcceptor;
import infra.remoting.util.DefaultPayload;
import infra.remoting.util.EmptyPayload;
import reactor.core.publisher.Flux;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.test.StepVerifier;

public class WebSocketTransportIntegrationTests {

  @Test
  public void sendStreamOfDataWithExternalHttpServerTest() {
    ConnectionAcceptor acceptor = RemotingServer.create(ChannelAcceptor.forRequestStream(
                    payload -> Flux.range(0, 10).map(i -> DefaultPayload.create(String.valueOf(i)))))
            .asConnectionAcceptor();

    DisposableServer server = HttpServer.create()
            .host("localhost")
            .route(router -> router.ws("/test", WebsocketRouteTransport.newHandler(acceptor)))
            .bindNow();

    Channel rsocket = ChannelConnector.connectWith(WebsocketClientTransport.create(URI.create("ws://" + server.host() + ":" + server.port() + "/test")))
            .block();

    StepVerifier.create(rsocket.requestStream(EmptyPayload.INSTANCE))
            .expectSubscription()
            .expectNextCount(10)
            .expectComplete()
            .verify(Duration.ofMillis(1000));
  }
}
