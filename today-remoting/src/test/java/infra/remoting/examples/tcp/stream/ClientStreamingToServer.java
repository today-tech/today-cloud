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

package infra.remoting.examples.tcp.stream;

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

public final class ClientStreamingToServer {

  private static final Logger logger = LoggerFactory.getLogger(ClientStreamingToServer.class);

  public static void main(String[] args) throws InterruptedException {
    RemotingServer.create(
                    ChannelAcceptor.forRequestStream(
                            payload ->
                                    Flux.interval(Duration.ofMillis(100))
                                            .map(aLong -> DefaultPayload.create("Interval: " + aLong))))
            .bindNow(TcpServerTransport.create("localhost", 7000));

    Channel channel =
            ChannelConnector.create()
                    .setupPayload(DefaultPayload.create("test", "test"))
                    .connect(TcpClientTransport.create("localhost", 7000))
                    .block();

    final Payload payload = DefaultPayload.create("Hello");
    channel
            .requestStream(payload)
            .map(Payload::getDataUtf8)
            .doOnNext(logger::debug)
            .take(10)
            .then()
            .doFinally(signalType -> channel.dispose())
            .then()
            .block();

    Thread.sleep(1000000);
  }
}
