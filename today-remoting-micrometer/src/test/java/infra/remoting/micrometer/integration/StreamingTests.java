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

package infra.remoting.micrometer.integration;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import infra.remoting.ChannelAcceptor;
import infra.remoting.Closeable;
import infra.remoting.Payload;
import infra.remoting.core.ChannelConnector;
import infra.remoting.core.RemotingServer;
import infra.remoting.error.ApplicationErrorException;
import infra.remoting.transport.local.LocalClientTransport;
import infra.remoting.transport.local.LocalServerTransport;
import infra.remoting.util.DefaultPayload;
import reactor.core.publisher.Flux;

class StreamingTests {
  LocalServerTransport serverTransport = LocalServerTransport.create("test");

  @Test
  public void testRangeButThrowException() {
    Closeable server = null;
    try {
      server = RemotingServer.create(ChannelAcceptor.forRequestStream(payload ->
                      Flux.range(1, 1000)
                              .doOnNext(i -> {
                                if (i > 3) {
                                  throw new RuntimeException("BOOM!");
                                }
                              })
                              .map((Function<? super Integer, ?>) l -> DefaultPayload.create("l -> " + l))
                              .cast(Payload.class)))
              .bind(serverTransport)
              .block();

      Assertions.assertThatThrownBy(
                      Flux.range(1, 6).flatMap(i -> consumer("connection number -> " + i))::blockLast)
              .isInstanceOf(ApplicationErrorException.class);

    }
    finally {
      server.dispose();
    }
  }

  @Test
  public void testRangeOfConsumers() {
    Closeable server = null;
    try {
      server =
              RemotingServer.create(
                              ChannelAcceptor.forRequestStream(
                                      payload ->
                                              Flux.range(1, 1000)
                                                      .map((Function<? super Integer, ?>) l -> DefaultPayload.create("l -> " + l))
                                                      .cast(Payload.class)))
                      .bind(serverTransport)
                      .block();

      Flux.range(1, 6).flatMap(i -> consumer("connection number -> " + i)).blockLast();
    }
    finally {
      server.dispose();
    }
  }

  private Flux<Payload> consumer(String s) {
    return ChannelConnector.connectWith(LocalClientTransport.create("test"))
            .flatMapMany(channel -> {
              AtomicInteger count = new AtomicInteger();
              return Flux.range(1, 100)
                      .flatMap(
                              i -> channel.requestStream(DefaultPayload.create("i -> " + i)).take(100), 1);
            });
  }

  @Test
  public void testSingleConsumer() {
    Closeable server = null;
    try {
      server =
              RemotingServer.create(ChannelAcceptor.forRequestStream(payload -> Flux.range(1, 10_000)
                              .map((Function<? super Integer, ?>) l -> DefaultPayload.create("l -> " + l))
                              .cast(Payload.class)))
                      .bind(serverTransport)
                      .block();

      consumer("1").blockLast();

    }
    finally {
      server.dispose();
    }
  }

  @Test
  public void testFluxOnly() {
    Flux<Long> longFlux = Flux.interval(Duration.ofMillis(1)).onBackpressureDrop();

    Flux.range(1, 60).flatMap(i -> longFlux.take(1000)).blockLast();
  }
}
