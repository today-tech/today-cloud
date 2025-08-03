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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetSocketAddress;
import java.net.URI;

import infra.remoting.transport.netty.server.WebsocketServerTransport;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

@ExtendWith(MockitoExtension.class)
class WebsocketClientTransportTests {

  @DisplayName("connects to server")
  @Test
  void connect() {
    InetSocketAddress address = InetSocketAddress.createUnresolved("localhost", 0);

    WebsocketServerTransport serverTransport = WebsocketServerTransport.create(address);

    serverTransport
            .start(duplexConnection -> Mono.empty())
            .flatMap(context -> WebsocketClientTransport.create(context.address()).connect())
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();
  }

  @DisplayName("create generates error if server not started")
  @Test
  void connectNoServer() {
    WebsocketClientTransport.create(8000).connect().as(StepVerifier::create).verifyError();
  }

  @DisplayName("creates client with BindAddress")
  @Test
  void createBindAddress() {
    assertThat(WebsocketClientTransport.create("test-bind-address", 8000))
            .isNotNull()
            .hasFieldOrPropertyWithValue("path", "/");
  }

  @DisplayName("creates client with HttpClient")
  @Test
  void createHttpClient() {
    assertThat(WebsocketClientTransport.create(HttpClient.create(), "/"))
            .isNotNull()
            .hasFieldOrPropertyWithValue("path", "/");
  }

  @DisplayName("creates client with HttpClient and path without root")
  @Test
  void createHttpClientWithPathWithoutRoot() {
    assertThat(WebsocketClientTransport.create(HttpClient.create(), "test"))
            .isNotNull()
            .hasFieldOrPropertyWithValue("path", "/test");
  }

  @DisplayName("creates client with InetSocketAddress")
  @Test
  void createInetSocketAddress() {
    assertThat(
            WebsocketClientTransport.create(
                    InetSocketAddress.createUnresolved("test-bind-address", 8000)))
            .isNotNull()
            .hasFieldOrPropertyWithValue("path", "/");
  }

  @DisplayName("create throws NullPointerException with null bindAddress")
  @Test
  void createNullBindAddress() {
    assertThatNullPointerException()
            .isThrownBy(() -> WebsocketClientTransport.create(null, 8000))
            .withMessage("host");
  }

  @DisplayName("create throws NullPointerException with null client")
  @Test
  void createNullHttpClient() {
    assertThatNullPointerException()
            .isThrownBy(() -> WebsocketClientTransport.create(null, "/test-path"))
            .withMessage("HttpClient is required");
  }

  @DisplayName("create throws NullPointerException with null address")
  @Test
  void createNullInetSocketAddress() {
    assertThatNullPointerException()
            .isThrownBy(() -> WebsocketClientTransport.create((InetSocketAddress) null))
            .withMessage("address is required");
  }

  @DisplayName("create throws NullPointerException with null path")
  @Test
  void createNullPath() {
    assertThatNullPointerException()
            .isThrownBy(() -> WebsocketClientTransport.create(HttpClient.create(), null))
            .withMessage("path is required");
  }

  @DisplayName("create throws NullPointerException with null URI")
  @Test
  void createNullUri() {
    assertThatNullPointerException()
            .isThrownBy(() -> WebsocketClientTransport.create((URI) null))
            .withMessage("uri is required");
  }

  @DisplayName("creates client with port")
  @Test
  void createPort() {
    assertThat(WebsocketClientTransport.create(8000)).isNotNull();
  }

  @DisplayName("creates client with URI")
  @Test
  void createUri() {
    assertThat(WebsocketClientTransport.create(URI.create("ws://test-host")))
            .isNotNull()
            .hasFieldOrPropertyWithValue("path", "/");
  }

  @DisplayName("creates client with URI path")
  @Test
  void createUriPath() {
    assertThat(WebsocketClientTransport.create(URI.create("ws://test-host/test")))
            .isNotNull()
            .hasFieldOrPropertyWithValue("path", "/test");
  }
}
