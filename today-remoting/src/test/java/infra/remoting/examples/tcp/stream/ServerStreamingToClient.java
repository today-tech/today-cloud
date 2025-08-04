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

import infra.remoting.Channel;
import infra.remoting.Payload;
import infra.remoting.core.ChannelConnector;
import infra.remoting.core.RemotingServer;
import infra.remoting.transport.netty.client.TcpClientTransport;
import infra.remoting.transport.netty.server.TcpServerTransport;
import infra.remoting.util.DefaultPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static infra.remoting.ChannelAcceptor.forRequestStream;

public final class ServerStreamingToClient {

  public static void main(String[] args) {

    RemotingServer.create((setup, channel) -> {
              channel
                      .requestStream(DefaultPayload.create("Hello-Bidi"))
                      .map(Payload::getDataUtf8)
                      .log()
                      .subscribe();

              return Mono.just(new Channel() { });
            })
            .bindNow(TcpServerTransport.create("localhost", 7000));

    Channel channel =
            ChannelConnector.create()
                    .acceptor(forRequestStream(payload ->
                            Flux.interval(Duration.ofSeconds(1))
                                    .map(aLong -> DefaultPayload.create("Bi-di Response => " + aLong))))
                    .connect(TcpClientTransport.create("localhost", 7000))
                    .block();

    channel.onClose().block();
  }
}
