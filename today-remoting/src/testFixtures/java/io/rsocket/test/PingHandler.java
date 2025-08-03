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

package io.rsocket.test;

import java.util.concurrent.ThreadLocalRandom;

import io.rsocket.ConnectionSetupPayload;
import io.rsocket.Payload;
import io.rsocket.Channel;
import io.rsocket.ChannelAcceptor;
import io.rsocket.util.ByteBufPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class PingHandler implements ChannelAcceptor {

  private final Payload pong;

  public PingHandler() {
    byte[] data = new byte[1024];
    ThreadLocalRandom.current().nextBytes(data);
    pong = ByteBufPayload.create(data);
  }

  public PingHandler(byte[] data) {
    pong = ByteBufPayload.create(data);
  }

  @Override
  public Mono<Channel> accept(ConnectionSetupPayload setup, Channel sendingSocket) {
    return Mono.just(
            new Channel() {
              @Override
              public Mono<Payload> requestResponse(Payload payload) {
                payload.release();
                return Mono.just(pong.retain());
              }

              @Override
              public Flux<Payload> requestStream(Payload payload) {
                payload.release();
                return Flux.range(0, 100).map(v -> pong.retain());
              }
            });
  }
}
