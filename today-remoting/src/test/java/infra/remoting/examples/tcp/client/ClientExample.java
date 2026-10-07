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
