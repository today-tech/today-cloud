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
