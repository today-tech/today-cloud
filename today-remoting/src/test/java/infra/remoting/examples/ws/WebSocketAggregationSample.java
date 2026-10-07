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

package infra.remoting.examples.ws;

import java.time.Duration;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.remoting.Channel;
import infra.remoting.ChannelAcceptor;
import infra.remoting.core.ChannelConnector;
import infra.remoting.core.RemotingServer;
import infra.remoting.frame.decoder.PayloadDecoder;
import infra.remoting.transport.ConnectionAcceptor;
import infra.remoting.transport.websocket.WebsocketClientTransport;
import infra.remoting.transport.websocket.WebsocketConnection;
import infra.remoting.util.ByteBufPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

public class WebSocketAggregationSample {

  private static final Logger logger = LoggerFactory.getLogger(WebSocketAggregationSample.class);

  public static void main(String[] args) {

    ConnectionAcceptor connectionAcceptor =
            RemotingServer.create(ChannelAcceptor.forRequestResponse(Mono::just))
                    .payloadDecoder(PayloadDecoder.ZERO_COPY)
                    .asConnectionAcceptor();

    DisposableServer server =
            HttpServer.create()
                    .host("localhost")
                    .port(0)
                    .handle((req, res) -> res.sendWebsocket((in, out) -> connectionAcceptor
                            .accept(new WebsocketConnection(
                                    (Connection) in.aggregateFrames()))
                            .then(out.neverComplete())))
                    .bindNow();

    WebsocketClientTransport transport =
            WebsocketClientTransport.create(server.host(), server.port());

    Channel clientChannel =
            ChannelConnector.create()
                    .keepAlive(Duration.ofMinutes(10), Duration.ofMinutes(10))
                    .payloadDecoder(PayloadDecoder.ZERO_COPY)
                    .connect(transport)
                    .block();

    Flux.range(1, 100)
            .concatMap(i -> clientChannel.requestResponse(ByteBufPayload.create("Hello " + i)))
            .doOnNext(payload -> logger.debug("Processed " + payload.getDataUtf8()))
            .blockLast();
    clientChannel.dispose();
    server.dispose();
  }
}
