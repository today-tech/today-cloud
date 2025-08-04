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
import infra.remoting.transport.netty.WebsocketDuplexConnection;
import infra.remoting.transport.netty.client.WebsocketClientTransport;
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
                            .accept(new WebsocketDuplexConnection(
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
