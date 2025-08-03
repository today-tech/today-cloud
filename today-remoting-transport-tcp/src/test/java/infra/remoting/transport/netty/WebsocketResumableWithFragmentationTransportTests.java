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

package infra.remoting.transport.netty;

import org.junit.jupiter.api.BeforeEach;

import java.net.InetSocketAddress;
import java.time.Duration;

import io.netty.channel.ChannelOption;
import infra.remoting.test.TransportPair;
import infra.remoting.test.TransportTest;
import infra.remoting.transport.netty.client.WebsocketClientTransport;
import infra.remoting.transport.netty.server.WebsocketServerTransport;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServer;

final class WebsocketResumableWithFragmentationTransportTests implements TransportTest {
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
                    true,
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
