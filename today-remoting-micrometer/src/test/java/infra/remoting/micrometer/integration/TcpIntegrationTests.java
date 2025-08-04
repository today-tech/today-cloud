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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import infra.remoting.Channel;
import infra.remoting.Payload;
import infra.remoting.core.ChannelConnector;
import infra.remoting.core.RemotingServer;
import infra.remoting.transport.netty.client.TcpClientTransport;
import infra.remoting.transport.netty.server.CloseableChannel;
import infra.remoting.transport.netty.server.TcpServerTransport;
import infra.remoting.util.ChannelWrapper;
import infra.remoting.util.DefaultPayload;
import infra.remoting.util.EmptyPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import static org.assertj.core.api.Assertions.assertThat;

public class TcpIntegrationTests {
  private Channel handler;

  private CloseableChannel server;

  @BeforeEach
  public void startup() {
    server = RemotingServer.create((setup, sendingSocket) -> Mono.just(new ChannelWrapper(handler)))
            .bind(TcpServerTransport.create("localhost", 0))
            .block();
  }

  private Channel buildClient() {
    return ChannelConnector.connectWith(TcpClientTransport.create(server.address())).block();
  }

  @AfterEach
  public void cleanup() {
    server.dispose();
  }

  @Test
  @Timeout(15_000L)
  public void testCompleteWithoutNext() {
    handler =
            new Channel() {
              @Override
              public Flux<Payload> requestStream(Payload payload) {
                return Flux.empty();
              }
            };
    Channel client = buildClient();
    Boolean hasElements =
            client.requestStream(DefaultPayload.create("REQUEST", "META")).log().hasElements().block();

    assertThat(hasElements).isFalse();
  }

  @Test
  @Timeout(15_000L)
  public void testSingleStream() {
    handler =
            new Channel() {
              @Override
              public Flux<Payload> requestStream(Payload payload) {
                return Flux.just(DefaultPayload.create("RESPONSE", "METADATA"));
              }
            };

    Channel client = buildClient();

    Payload result = client.requestStream(DefaultPayload.create("REQUEST", "META")).blockLast();

    assertThat(result.getDataUtf8()).isEqualTo("RESPONSE");
  }

  @Test
  @Timeout(15_000L)
  public void testZeroPayload() {
    handler =
            new Channel() {
              @Override
              public Flux<Payload> requestStream(Payload payload) {
                return Flux.just(EmptyPayload.INSTANCE);
              }
            };

    Channel client = buildClient();

    Payload result = client.requestStream(DefaultPayload.create("REQUEST", "META")).blockFirst();

    assertThat(result.getDataUtf8()).isEmpty();
  }

  @Test
  @Timeout(15_000L)
  public void testRequestResponseErrors() {
    handler =
            new Channel() {
              boolean first = true;

              @Override
              public Mono<Payload> requestResponse(Payload payload) {
                if (first) {
                  first = false;
                  return Mono.error(new RuntimeException("EX"));
                }
                else {
                  return Mono.just(DefaultPayload.create("SUCCESS"));
                }
              }
            };

    Channel client = buildClient();

    Payload response1 =
            client
                    .requestResponse(DefaultPayload.create("REQUEST", "META"))
                    .onErrorReturn(DefaultPayload.create("ERROR"))
                    .block();
    Payload response2 =
            client
                    .requestResponse(DefaultPayload.create("REQUEST", "META"))
                    .onErrorReturn(DefaultPayload.create("ERROR"))
                    .block();

    assertThat(response1.getDataUtf8()).isEqualTo("ERROR");
    assertThat(response2.getDataUtf8()).isEqualTo("SUCCESS");
  }

  @Test
  @Timeout(15_000L)
  public void testTwoConcurrentStreams() throws InterruptedException {
    ConcurrentHashMap<String, Sinks.Many<Payload>> map = new ConcurrentHashMap<>();
    Sinks.Many<Payload> processor1 = Sinks.many().unicast().onBackpressureBuffer();
    map.put("REQUEST1", processor1);
    Sinks.Many<Payload> processor2 = Sinks.many().unicast().onBackpressureBuffer();
    map.put("REQUEST2", processor2);

    handler = new Channel() {
      @Override
      public Flux<Payload> requestStream(Payload payload) {
        return map.get(payload.getDataUtf8()).asFlux();
      }
    };

    Channel client = buildClient();

    Flux<Payload> response1 = client.requestStream(DefaultPayload.create("REQUEST1"));
    Flux<Payload> response2 = client.requestStream(DefaultPayload.create("REQUEST2"));

    CountDownLatch nextCountdown = new CountDownLatch(2);
    CountDownLatch completeCountdown = new CountDownLatch(2);

    response1
            .subscribeOn(Schedulers.newSingle("1"))
            .subscribe(c -> nextCountdown.countDown(), t -> { }, completeCountdown::countDown);

    response2
            .subscribeOn(Schedulers.newSingle("2"))
            .subscribe(c -> nextCountdown.countDown(), t -> { }, completeCountdown::countDown);

    processor1.tryEmitNext(DefaultPayload.create("RESPONSE1A"));
    processor2.tryEmitNext(DefaultPayload.create("RESPONSE2A"));

    nextCountdown.await();

    processor1.tryEmitComplete();
    processor2.tryEmitComplete();

    completeCountdown.await();
  }
}
