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
