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

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.function.Supplier;

import infra.remoting.Channel;
import infra.remoting.ChannelAcceptor;
import infra.remoting.Payload;
import infra.remoting.core.ChannelConnector;
import infra.remoting.core.RemotingServer;
import infra.remoting.test.SlowTest;
import infra.remoting.transport.netty.client.TcpClientTransport;
import infra.remoting.transport.netty.server.CloseableChannel;
import infra.remoting.transport.netty.server.TcpServerTransport;
import infra.remoting.util.DefaultPayload;
import reactor.core.publisher.Flux;

public class InteractionsLoadTests {

  @Test
  @SlowTest
  public void channel() {
    CloseableChannel server = RemotingServer.create(ChannelAcceptor.with(new EchoChannel()))
            .bind(TcpServerTransport.create("localhost", 0))
            .block(Duration.ofSeconds(10));

    Channel clientChannel = ChannelConnector.connectWith(TcpClientTransport.create(server.address()))
            .block(Duration.ofSeconds(10));

    int concurrency = 16;
    Flux.range(1, concurrency)
            .flatMap(v -> clientChannel
                            .requestChannel(input().onBackpressureDrop().map(iv -> DefaultPayload.create("foo")))
                            .limitRate(10000),
                    concurrency)
            .timeout(Duration.ofSeconds(5))
            .doOnNext(p -> {
              String data = p.getDataUtf8();
              if (!data.equals("bar")) {
                throw new IllegalStateException("Channel Client Bad message: " + data);
              }
            })
            .window(Duration.ofSeconds(1))
            .flatMap(Flux::count)
            .doOnNext(d -> System.out.println("Got: " + d))
            .take(Duration.ofMinutes(1))
            .doOnTerminate(server::dispose)
            .subscribe();

    server.onClose().block();
  }

  private static Flux<Long> input() {
    Flux<Long> interval = Flux.interval(Duration.ofMillis(1)).onBackpressureDrop();
    for (int i = 0; i < 10; i++) {
      interval = interval.mergeWith(interval);
    }
    return interval;
  }

  private static class EchoChannel implements Channel {

    @Override
    public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
      return Flux.from(payloads)
              .map(p -> {
                String data = p.getDataUtf8();
                if (!data.equals("foo")) {
                  throw new IllegalStateException("Channel Server Bad message: " + data);
                }
                return DefaultPayload.create("bar");
              });
    }

    @Override
    public Flux<Payload> requestStream(Payload payload) {
      return Flux.just(payload)
              .map(Payload::getDataUtf8)
              .doOnNext((data) -> {
                if (!data.equals("foo")) {
                  throw new IllegalStateException("Stream Server Bad message: " + data);
                }
              })
              .flatMap(data -> {
                Supplier<Payload> p = () -> DefaultPayload.create("bar");
                return Flux.range(1, 100).map(v -> p.get());
              });
    }
  }
}
