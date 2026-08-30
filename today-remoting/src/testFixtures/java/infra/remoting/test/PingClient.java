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

package infra.remoting.test;

import org.HdrHistogram.Recorder;
import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.function.BiFunction;

import infra.remoting.Channel;
import infra.remoting.Payload;
import infra.remoting.util.ByteBufPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class PingClient {

  private final Payload payload;
  private final Mono<Channel> client;

  public PingClient(Mono<Channel> client) {
    this.client = client;
    this.payload = ByteBufPayload.create("hello");
  }

  public Recorder startTracker(Duration interval) {
    final Recorder histogram = new Recorder(3600000000000L, 3);
    Flux.interval(interval)
            .doOnNext(
                    aLong -> {
                      System.out.println("---- PING/ PONG HISTO ----");
                      histogram
                              .getIntervalHistogram()
                              .outputPercentileDistribution(System.out, 5, 1000.0, false);
                      System.out.println("---- PING/ PONG HISTO ----");
                    })
            .subscribe();
    return histogram;
  }

  public Flux<Payload> requestResponsePingPong(int count, final Recorder histogram) {
    return pingPong(Channel::requestResponse, count, histogram);
  }

  public Flux<Payload> requestStreamPingPong(int count, final Recorder histogram) {
    return pingPong(Channel::requestStream, count, histogram);
  }

  Flux<Payload> pingPong(
          BiFunction<Channel, ? super Payload, ? extends Publisher<Payload>> interaction,
          int count,
          final Recorder histogram) {
    return Flux.usingWhen(
                    client,
                    channel ->
                            Flux.range(1, count)
                                    .flatMap(
                                            i -> {
                                              long start = System.nanoTime();
                                              return Flux.from(interaction.apply(channel, payload.retain()))
                                                      .doOnNext(Payload::release)
                                                      .doFinally(
                                                              signalType -> {
                                                                long diff = System.nanoTime() - start;
                                                                histogram.recordValue(diff);
                                                              });
                                            },
                                            64),
                    channel -> {
                      channel.dispose();
                      return channel.onClose();
                    })
            .doOnError(Throwable::printStackTrace);
  }
}
