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

package infra.remoting.examples.tcp.client;

import java.time.Duration;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.remoting.Channel;
import infra.remoting.ChannelAcceptor;
import infra.remoting.Payload;
import infra.remoting.core.ChannelConnector;
import infra.remoting.core.RemotingClient;
import infra.remoting.core.RemotingServer;
import infra.remoting.transport.netty.client.TcpClientTransport;
import infra.remoting.transport.netty.server.TcpServerTransport;
import infra.remoting.util.DefaultPayload;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

public class ClientExample {
  static final Logger logger = LoggerFactory.getLogger(ClientExample.class);

  public static void main(String[] args) {
    RemotingServer.create(ChannelAcceptor.forRequestResponse(p -> {
              String data = p.getDataUtf8();
              logger.info("Received request data {}", data);

              Payload responsePayload = DefaultPayload.create("Echo: " + data);
              p.release();

              return Mono.just(responsePayload);
            }))
            .bind(TcpServerTransport.create("localhost", 7000))
            .delaySubscription(Duration.ofSeconds(5))
            .doOnNext(cc -> logger.info("Server started on the address : {}", cc.address()))
            .block();

    Mono<Channel> source =
            ChannelConnector.create()
                    .reconnect(Retry.backoff(50, Duration.ofMillis(500)))
                    .connect(TcpClientTransport.create("localhost", 7000));

    RemotingClient.from(source)
            .requestResponse(Mono.just(DefaultPayload.create("Test Request")))
            .doOnSubscribe(s -> logger.info("Executing Request"))
            .doOnNext(
                    d -> {
                      logger.info("Received response data {}", d.getDataUtf8());
                      d.release();
                    })
            .repeat(10)
            .blockLast();
  }
}
