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

import org.HdrHistogram.Recorder;
import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.function.BiFunction;

import io.rsocket.Payload;
import io.rsocket.Channel;
import io.rsocket.util.ByteBufPayload;
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
                    rsocket ->
                            Flux.range(1, count)
                                    .flatMap(
                                            i -> {
                                              long start = System.nanoTime();
                                              return Flux.from(interaction.apply(rsocket, payload.retain()))
                                                      .doOnNext(Payload::release)
                                                      .doFinally(
                                                              signalType -> {
                                                                long diff = System.nanoTime() - start;
                                                                histogram.recordValue(diff);
                                                              });
                                            },
                                            64),
                    rsocket -> {
                      rsocket.dispose();
                      return rsocket.onClose();
                    })
            .doOnError(Throwable::printStackTrace);
  }
}
