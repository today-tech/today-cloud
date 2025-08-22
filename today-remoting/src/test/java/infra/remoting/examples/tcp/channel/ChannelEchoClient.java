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

package infra.remoting.examples.tcp.channel;

import java.time.Duration;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.remoting.Channel;
import infra.remoting.ChannelAcceptor;
import infra.remoting.Payload;
import infra.remoting.core.ChannelConnector;
import infra.remoting.core.RemotingServer;
import infra.remoting.transport.netty.client.TcpClientTransport;
import infra.remoting.transport.netty.server.TcpServerTransport;
import infra.remoting.util.DefaultPayload;
import reactor.core.publisher.Flux;

public final class ChannelEchoClient {

  private static final Logger logger = LoggerFactory.getLogger(ChannelEchoClient.class);

  public static void main(String[] args) {

    ChannelAcceptor echoAcceptor =
            ChannelAcceptor.forRequestChannel(
                    payloads ->
                            Flux.from(payloads)
                                    .map(Payload::getDataUtf8)
                                    .map(s -> "Echo: " + s)
                                    .map(DefaultPayload::create));

    RemotingServer.create(echoAcceptor).bindNow(TcpServerTransport.create("localhost", 7000));

    Channel channel =
            ChannelConnector.connectWith(TcpClientTransport.create("localhost", 7000)).block();

    channel
            .requestChannel(
                    Flux.interval(Duration.ofMillis(1000)).map(i -> DefaultPayload.create("Hello")))
            .map(Payload::getDataUtf8)
            .doOnNext(logger::debug)
            .take(10)
            .doFinally(signalType -> channel.dispose())
            .then()
            .block();
  }
}
