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

import java.time.Duration;

import io.rsocket.Closeable;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

final class LocalClientTransportTests {

  @DisplayName("connects to server")
  @Test
  void connect() {
    LocalServerTransport serverTransport = LocalServerTransport.createEphemeral();

    Closeable closeable =
            serverTransport.start(duplexConnection -> duplexConnection.receive().then()).block();

    try {
      LocalClientTransport.create(serverTransport.getName())
              .connect()
              .doOnNext(d -> d.receive().subscribe())
              .as(StepVerifier::create)
              .expectNextCount(1)
              .verifyComplete();
    }
    finally {
      closeable.dispose();
      closeable.onClose().block(Duration.ofSeconds(5));
    }
  }

  @DisplayName("generates error if server not started")
  @Test
  void connectNoServer() {
    LocalClientTransport.create("test-name")
            .connect()
            .as(StepVerifier::create)
            .verifyErrorMessage("Could not find server: test-name");
  }

  @DisplayName("creates client")
  @Test
  void create() {
    assertThat(LocalClientTransport.create("test-name")).isNotNull();
  }

  @DisplayName("throws NullPointerException with null name")
  @Test
  void createNullName() {
    assertThatNullPointerException()
            .isThrownBy(() -> LocalClientTransport.create(null))
            .withMessage("name is required");
  }
}
