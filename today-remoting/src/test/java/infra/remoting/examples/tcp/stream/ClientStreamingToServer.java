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
