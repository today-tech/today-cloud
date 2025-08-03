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

import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import io.rsocket.Payload;
import io.rsocket.Channel;
import io.rsocket.util.ByteBufPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static java.util.concurrent.locks.LockSupport.parkNanos;

public class TestChannel implements Channel {
  private final String data;
  private final String metadata;

  private final AtomicLong observedInteractions = new AtomicLong();
  private final AtomicLong activeInteractions = new AtomicLong();

  public TestChannel(String data, String metadata) {
    this.data = data;
    this.metadata = metadata;
  }

  @Override
  public Mono<Payload> requestResponse(Payload payload) {
    activeInteractions.getAndIncrement();
    payload.release();
    observedInteractions.getAndIncrement();
    return Mono.just(ByteBufPayload.create(data, metadata))
            .doFinally(__ -> activeInteractions.getAndDecrement());
  }

  @Override
  public Flux<Payload> requestStream(Payload payload) {
    activeInteractions.getAndIncrement();
    payload.release();
    observedInteractions.getAndIncrement();
    return Flux.range(1, 10_000)
            .map(l -> ByteBufPayload.create(data, metadata))
            .doFinally(__ -> activeInteractions.getAndDecrement());
  }

  @Override
  public Mono<Void> metadataPush(Payload payload) {
    activeInteractions.getAndIncrement();
    payload.release();
    observedInteractions.getAndIncrement();
    return Mono.<Void>empty().doFinally(__ -> activeInteractions.getAndDecrement());
  }

  @Override
  public Mono<Void> fireAndForget(Payload payload) {
    activeInteractions.getAndIncrement();
    payload.release();
    observedInteractions.getAndIncrement();
    return Mono.<Void>empty().doFinally(__ -> activeInteractions.getAndDecrement());
  }

  @Override
  public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
    activeInteractions.getAndIncrement();
    observedInteractions.getAndIncrement();
    return Flux.from(payloads).doFinally(__ -> activeInteractions.getAndDecrement());
  }

  public boolean awaitAllInteractionTermination(Duration duration) {
    long end = duration.plusNanos(System.nanoTime()).toNanos();
    long activeNow;
    while ((activeNow = activeInteractions.get()) > 0) {
      if (System.nanoTime() >= end) {
        return false;
      }
      parkNanos(100);
    }

    return activeNow == 0;
  }

  public boolean awaitUntilObserved(int interactions, Duration duration) {
    long end = System.nanoTime() + duration.toNanos();
    long observed;
    while ((observed = observedInteractions.get()) < interactions) {
      if (System.nanoTime() >= end) {
        return false;
      }
      parkNanos(100);
    }

    return observed >= interactions;
  }
}
