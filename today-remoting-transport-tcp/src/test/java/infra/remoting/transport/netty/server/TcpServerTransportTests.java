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
