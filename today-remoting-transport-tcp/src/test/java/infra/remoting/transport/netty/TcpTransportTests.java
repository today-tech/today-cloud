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
import infra.remoting.transport.netty.client.TcpClientTransport;
import infra.remoting.transport.netty.server.TcpServerTransport;
import reactor.netty.tcp.TcpClient;
import reactor.netty.tcp.TcpServer;

final class TcpTransportTests implements TransportTest {
  private TransportPair transportPair;

  @BeforeEach
  void createTestPair() {
    transportPair =
            new TransportPair<>(
                    () -> InetSocketAddress.createUnresolved("localhost", 0),
                    (address, server, allocator) ->
                            TcpClientTransport.create(
                                    TcpClient.create()
                                            .remoteAddress(server::address)
                                            .option(ChannelOption.ALLOCATOR, allocator)),
                    (address, allocator) -> {
                      return TcpServerTransport.create(
                              TcpServer.create()
                                      .bindAddress(() -> address)
                                      .option(ChannelOption.ALLOCATOR, allocator));
                    });
  }

  @Override
  public Duration getTimeout() {
    return Duration.ofMinutes(2);
  }

  @Override
  public TransportPair getTransportPair() {
    return transportPair;
  }
}
