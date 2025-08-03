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

package infra.remoting.transport.netty.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import infra.remoting.transport.netty.server.TcpServerTransport;
import reactor.core.publisher.Mono;
import reactor.netty.tcp.TcpClient;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class TcpClientTransportTests {

  @DisplayName("connects to server")
  @Test
  void connect() {
    InetSocketAddress address = InetSocketAddress.createUnresolved("localhost", 0);

    TcpServerTransport serverTransport = TcpServerTransport.create(address);

    serverTransport
            .start(duplexConnection -> Mono.empty())
            .flatMap(context -> TcpClientTransport.create(context.address()).connect())
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();
  }

  @DisplayName("create generates error if server not started")
  @Test
  void connectNoServer() {
    TcpClientTransport.create(8000).connect().as(StepVerifier::create).verifyError();
  }

  @DisplayName("creates client with BindAddress")
  @Test
  void createBindAddress() {
    assertThat(TcpClientTransport.create("test-bind-address", 8000)).isNotNull();
  }

  @DisplayName("creates client with InetSocketAddress")
  @Test
  void createInetSocketAddress() {
    assertThat(
            TcpClientTransport.create(
                    InetSocketAddress.createUnresolved("test-bind-address", 8000)))
            .isNotNull();
  }

  @DisplayName("create throws NullPointerException with null bindAddress")
  @Test
  void createNullBindAddress() {
    assertThatNullPointerException()
            .isThrownBy(() -> TcpClientTransport.create((String) null, 8000))
            .withMessage("bindAddress is required");
  }

  @DisplayName("create throws NullPointerException with null address")
  @Test
  void createNullInetSocketAddress() {
    assertThatNullPointerException()
            .isThrownBy(() -> TcpClientTransport.create((InetSocketAddress) null))
            .withMessage("address is required");
  }

  @DisplayName("create throws NullPointerException with null client")
  @Test
  void createNullTcpClient() {
    assertThatNullPointerException()
            .isThrownBy(() -> TcpClientTransport.create((TcpClient) null))
            .withMessage("client is required");
  }

  @DisplayName("creates client with port")
  @Test
  void createPort() {
    assertThat(TcpClientTransport.create(8000)).isNotNull();
  }

  @DisplayName("creates client with TcpClient")
  @Test
  void createTcpClient() {
    assertThat(TcpClientTransport.create(TcpClient.create())).isNotNull();
  }
}
