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

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

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

    Channel channel = ChannelConnector.connectWith(WebsocketClientTransport.create(URI.create("ws://" + server.host() + ":" + server.port() + "/test")))
            .block();

    StepVerifier.create(channel.requestStream(EmptyPayload.INSTANCE))
            .expectSubscription()
            .expectNextCount(10)
            .expectComplete()
            .verify(Duration.ofMillis(1000));
  }
}
