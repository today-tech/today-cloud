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

package infra.remoting.transport.netty.server;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import reactor.core.publisher.Mono;
import reactor.netty.tcp.TcpServer;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class TcpServerTransportTests {

  @DisplayName("creates server with BindAddress")
  @Test
  void createBindAddress() {
    assertThat(TcpServerTransport.create("test-bind-address", 8000)).isNotNull();
  }

  @DisplayName("creates server with InetSocketAddress")
  @Test
  void createInetSocketAddress() {
    assertThat(
            TcpServerTransport.create(
                    InetSocketAddress.createUnresolved("test-bind-address", 8000)))
            .isNotNull();
  }

  @DisplayName("create throws NullPointerException with null bindAddress")
  @Test
  void createNullBindAddress() {
    assertThatNullPointerException()
            .isThrownBy(() -> TcpServerTransport.create((String) null, 8000))
            .withMessage("bindAddress is required");
  }

  @DisplayName("create throws NullPointerException with null address")
  @Test
  void createNullInetSocketAddress() {
    assertThatNullPointerException()
            .isThrownBy(() -> TcpServerTransport.create((InetSocketAddress) null))
            .withMessage("address is required");
  }

  @DisplayName("create throws NullPointerException with null server")
  @Test
  void createNullTcpClient() {
    assertThatNullPointerException()
            .isThrownBy(() -> TcpServerTransport.create((TcpServer) null))
            .withMessage("server is required");
  }

  @DisplayName("creates server with port")
  @Test
  void createPort() {
    assertThat(TcpServerTransport.create("localhost", 8000)).isNotNull();
  }

  @DisplayName("creates client with TcpServer")
  @Test
  void createTcpClient() {
    assertThat(TcpServerTransport.create(TcpServer.create())).isNotNull();
  }

  @DisplayName("starts server")
  @Test
  void start() {
    InetSocketAddress address = InetSocketAddress.createUnresolved("localhost", 0);

    TcpServerTransport serverTransport = TcpServerTransport.create(address);

    serverTransport
            .start(duplexConnection -> Mono.empty())
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();
  }

  @DisplayName("start throws NullPointerException with null acceptor")
  @Test
  void startNullAcceptor() {
    assertThatNullPointerException()
            .isThrownBy(() -> TcpServerTransport.create("localhost", 8000).start(null))
            .withMessage("acceptor is required");
  }
}
