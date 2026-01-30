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

package infra.remoting.transport.websocket;

import org.junit.jupiter.api.BeforeEach;

import java.net.InetSocketAddress;
import java.time.Duration;

import infra.remoting.test.TransportPair;
import infra.remoting.test.TransportTest;
import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServer;

final class WebsocketResumableTransportTests implements TransportTest {
  private TransportPair transportPair;

  @BeforeEach
  void createTestPair() {
    transportPair =
            new TransportPair<>(
                    () -> InetSocketAddress.createUnresolved("localhost", 0),
                    (address, server, allocator) ->
                            WebsocketClientTransport.create(
                                    HttpClient.create()
                                            .host(server.address().getHostName())
                                            .port(server.address().getPort())
                                            .option(ChannelOption.ALLOCATOR, allocator),
                                    ""),
                    (address, allocator) -> {
                      return WebsocketServerTransport.create(
                              HttpServer.create()
                                      .host(address.getHostName())
                                      .port(address.getPort())
                                      .option(ChannelOption.ALLOCATOR, allocator));
                    },
                    false,
                    true);
  }

  @Override
  public Duration getTimeout() {
    return Duration.ofMinutes(3);
  }

  @Override
  public TransportPair getTransportPair() {
    return transportPair;
  }
}
