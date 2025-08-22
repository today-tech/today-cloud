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

package infra.remoting.examples.tcp.requestresponse;

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
import reactor.core.publisher.Mono;

public final class HelloWorldClient {

  private static final Logger logger = LoggerFactory.getLogger(HelloWorldClient.class);

  public static void main(String[] args) {

    Channel channel = new Channel() {
      boolean fail = true;

      @Override
      public Mono<Payload> requestResponse(Payload p) {
        if (fail) {
          fail = false;
          return Mono.error(new Throwable("Simulated error"));
        }
        else {
          return Mono.just(p);
        }
      }
    };

    RemotingServer.create(ChannelAcceptor.with(channel))
            .bindNow(TcpServerTransport.create("localhost", 7000));

    Channel channel1 =
            ChannelConnector.connectWith(TcpClientTransport.create("localhost", 7000)).block();

    for (int i = 0; i < 3; i++) {
      channel1
              .requestResponse(DefaultPayload.create("Hello"))
              .map(Payload::getDataUtf8)
              .onErrorReturn("error")
              .doOnNext(logger::debug)
              .block();
    }

    channel1.dispose();
  }
}
