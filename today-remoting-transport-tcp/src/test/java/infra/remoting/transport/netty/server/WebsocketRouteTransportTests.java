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

import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServer;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class WebsocketRouteTransportTests {

  @DisplayName("creates server")
  @Test
  void constructor() {
    new WebsocketRouteTransport(HttpServer.create(), routes -> { }, "/test-path");
  }

  @DisplayName("constructor throw NullPointer with null path")
  @Test
  void constructorNullPath() {
    assertThatNullPointerException()
            .isThrownBy(() -> new WebsocketRouteTransport(HttpServer.create(), routes -> { }, null))
            .withMessage("path is required");
  }

  @DisplayName("constructor throw NullPointer with null routesBuilder")
  @Test
  void constructorNullRoutesBuilder() {
    assertThatNullPointerException()
            .isThrownBy(() -> new WebsocketRouteTransport(HttpServer.create(), null, "/test-path"))
            .withMessage("routesBuilder is required");
  }

  @DisplayName("constructor throw NullPointer with null server")
  @Test
  void constructorNullServer() {
    assertThatNullPointerException()
            .isThrownBy(() -> new WebsocketRouteTransport(null, routes -> { }, "/test-path"))
            .withMessage("server is required");
  }

  @DisplayName("starts server")
  @Test
  void start() {
    WebsocketRouteTransport serverTransport =
            new WebsocketRouteTransport(HttpServer.create(), routes -> { }, "/test-path");

    serverTransport
            .start(duplexConnection -> Mono.empty())
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();
  }

  @DisplayName("start throw NullPointerException with null acceptor")
  @Test
  void startNullAcceptor() {
    assertThatNullPointerException()
            .isThrownBy(
                    () ->
                            new WebsocketRouteTransport(HttpServer.create(), routes -> { }, "/test-path")
                                    .start(null))
            .withMessage("acceptor is required");
  }
}
