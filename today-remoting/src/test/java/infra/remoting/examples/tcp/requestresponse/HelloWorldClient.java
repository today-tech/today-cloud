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
