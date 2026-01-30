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
