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

package infra.remoting.transport.local;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

final class LocalServerTransportTests {

  @DisplayName("create throws NullPointerException with null name")
  @Test
  void createNullName() {
    assertThatNullPointerException()
            .isThrownBy(() -> LocalServerTransport.create(null))
            .withMessage("name is required");
  }

  @DisplayName("dispose removes name from registry")
  @Test
  void dispose() {
    LocalServerTransport.dispose("test-name");
  }

  @DisplayName("dispose throws NullPointerException with null name")
  @Test
  void disposeNullName() {
    assertThatNullPointerException()
            .isThrownBy(() -> LocalServerTransport.dispose(null))
            .withMessage("name is required");
  }

  @DisplayName("creates transports with ephemeral names")
  @Test
  void ephemeral() {
    LocalServerTransport serverTransport1 = LocalServerTransport.createEphemeral();
    LocalServerTransport serverTransport2 = LocalServerTransport.createEphemeral();

    assertThat(serverTransport1.getName()).isNotEqualTo(serverTransport2.getName());
  }

  @DisplayName("returns the server by name")
  @Test
  void findServer() {
    LocalServerTransport serverTransport = LocalServerTransport.createEphemeral();

    serverTransport
            .start(duplexConnection -> Mono.empty())
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();

    assertThat(LocalServerTransport.findServer(serverTransport.getName())).isNotNull();
  }

  @DisplayName("returns null if server hasn't been started")
  @Test
  void findServerMissingName() {
    assertThat(LocalServerTransport.findServer("test-name")).isNull();
  }

  @DisplayName("findServer throws NullPointerException with null name")
  @Test
  void findServerNullName() {
    assertThatNullPointerException()
            .isThrownBy(() -> LocalServerTransport.findServer(null))
            .withMessage("name is required");
  }

  @DisplayName("creates transport with name")
  @Test
  void named() {
    LocalServerTransport serverTransport = LocalServerTransport.create("test-name");

    assertThat(serverTransport.getName()).isEqualTo("test-name");
  }

  @DisplayName("starts local server transport")
  @Test
  void start() {
    LocalServerTransport ephemeral = LocalServerTransport.createEphemeral();
    try {
      ephemeral
              .start(duplexConnection -> Mono.empty())
              .as(StepVerifier::create)
              .expectNextCount(1)
              .verifyComplete();
    }
    finally {
      LocalServerTransport.dispose(ephemeral.getName());
    }
  }

  @DisplayName("start throws NullPointerException with null acceptor")
  @Test
  void startNullAcceptor() {
    assertThatNullPointerException()
            .isThrownBy(() -> LocalServerTransport.createEphemeral().start(null))
            .withMessage("acceptor is required");
  }
}
