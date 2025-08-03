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

import io.netty.channel.Channel;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableChannel;
import reactor.netty.tcp.TcpServer;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class CloseableChannelTests {

  private final Mono<? extends DisposableChannel> channel =
          TcpServer.create().handle((in, out) -> Mono.empty()).bind();

  @DisplayName("returns the address of the context")
  @Test
  void address() {
    channel
            .map(CloseableChannel::new)
            .map(CloseableChannel::address)
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();
  }

  @DisplayName("creates instance")
  @Test
  void constructor() {
    channel.map(CloseableChannel::new).as(StepVerifier::create).expectNextCount(1).verifyComplete();
  }

  @DisplayName("constructor throws NullPointerException with null context")
  @Test
  void constructorNullContext() {
    assertThatNullPointerException()
            .isThrownBy(() -> new CloseableChannel((Channel) null))
            .withMessage("channel is required");
  }

  @DisplayName("disposes context")
  @Test
  void dispose() {
    channel
            .map(CloseableChannel::new)
            .delayUntil(
                    closeable -> {
                      closeable.dispose();
                      return closeable.onClose().log();
                    })
            .as(StepVerifier::create)
            .assertNext(closeable -> assertThat(closeable.isDisposed()).isTrue())
            .verifyComplete();
  }
}
