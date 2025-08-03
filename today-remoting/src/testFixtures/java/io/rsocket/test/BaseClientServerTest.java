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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.atomic.AtomicInteger;

import io.rsocket.Payload;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class BaseClientServerTest<T extends ClientSetupRule<?, ?>> {
  public final T setup = createClientServer();

  protected abstract T createClientServer();

  @BeforeEach
  public void init() {
    setup.init();
  }

  @AfterEach
  public void teardown() {
    setup.tearDown();
  }

  @Test
  @Timeout(10000)
  public void testFireNForget10() {
    long outputCount =
            Flux.range(1, 10)
                    .flatMap(i -> setup.getRSocket().fireAndForget(testPayload(i)))
                    .doOnError(Throwable::printStackTrace)
                    .count()
                    .block();

    assertThat(outputCount).isZero();
  }

  @Test
  @Timeout(10000)
  public void testPushMetadata10() {
    long outputCount =
            Flux.range(1, 10)
                    .flatMap(i -> setup.getRSocket().metadataPush(DefaultPayload.create("", "metadata")))
                    .doOnError(Throwable::printStackTrace)
                    .count()
                    .block();

    assertThat(outputCount).isZero();
  }

  @Test // (timeout = 10000)
  public void testRequestResponse1() {
    long outputCount =
            Flux.range(1, 1)
                    .flatMap(
                            i -> setup.getRSocket().requestResponse(testPayload(i)).map(Payload::getDataUtf8))
                    .doOnError(Throwable::printStackTrace)
                    .count()
                    .block();

    assertThat(outputCount).isZero();
  }

  @Test
  @Timeout(10000)
  public void testRequestResponse10() {
    long outputCount =
            Flux.range(1, 10)
                    .flatMap(
                            i -> setup.getRSocket().requestResponse(testPayload(i)).map(Payload::getDataUtf8))
                    .doOnError(Throwable::printStackTrace)
                    .count()
                    .block();

    assertThat(outputCount).isEqualTo(10);
  }

  private Payload testPayload(int metadataPresent) {
    String metadata;
    switch (metadataPresent % 5) {
      case 0:
        metadata = null;
        break;
      case 1:
        metadata = "";
        break;
      default:
        metadata = "metadata";
        break;
    }
    return DefaultPayload.create("hello", metadata);
  }

  @Test
  @Timeout(10000)
  public void testRequestResponse100() {
    long outputCount =
            Flux.range(1, 100)
                    .flatMap(
                            i -> setup.getRSocket().requestResponse(testPayload(i)).map(Payload::getDataUtf8))
                    .doOnError(Throwable::printStackTrace)
                    .count()
                    .block();

    assertThat(outputCount).isEqualTo(100);
  }

  @Test
  @Timeout(20000)
  public void testRequestResponse10_000() {
    long outputCount =
            Flux.range(1, 10_000)
                    .flatMap(
                            i -> setup.getRSocket().requestResponse(testPayload(i)).map(Payload::getDataUtf8))
                    .doOnError(Throwable::printStackTrace)
                    .count()
                    .block();

    assertThat(outputCount).isEqualTo(10_000);
  }

  @Test
  @Timeout(10000)
  public void testRequestStream() {
    Flux<Payload> publisher = setup.getRSocket().requestStream(testPayload(3));

    long count = publisher.take(5).count().block();

    assertThat(count).isEqualTo(5);
  }

  @Test
  @Timeout(10000)
  public void testRequestStreamAll() {
    Flux<Payload> publisher = setup.getRSocket().requestStream(testPayload(3));

    long count = publisher.count().block();

    assertThat(count).isEqualTo(10000);
  }

  @Test
  @Timeout(10000)
  public void testRequestStreamWithRequestN() {
    CountdownBaseSubscriber ts = new CountdownBaseSubscriber();
    ts.expect(5);

    setup.getRSocket().requestStream(testPayload(3)).subscribe(ts);

    ts.await();
    assertThat(ts.count()).isEqualTo(5);

    ts.expect(5);
    ts.await();
    ts.cancel();

    assertThat(ts.count()).isEqualTo(10);
  }

  @Test
  @Timeout(10000)
  public void testRequestStreamWithDelayedRequestN() {
    CountdownBaseSubscriber ts = new CountdownBaseSubscriber();

    setup.getRSocket().requestStream(testPayload(3)).subscribe(ts);

    ts.expect(5);

    ts.await();
    assertThat(ts.count()).isEqualTo(5);

    ts.expect(5);
    ts.await();
    ts.cancel();

    assertThat(ts.count()).isEqualTo(10);
  }

  @Test
  @Timeout(10000)
  public void testChannel0() {
    Flux<Payload> publisher = setup.getRSocket().requestChannel(Flux.empty());

    long count = publisher.count().block();

    assertThat(count).isZero();
  }

  @Test
  @Timeout(10000)
  public void testChannel1() {
    Flux<Payload> publisher = setup.getRSocket().requestChannel(Flux.just(testPayload(0)));

    long count = publisher.count().block();

    assertThat(count).isOne();
  }

  @Test
  @Timeout(10000)
  public void testChannel3() {
    Flux<Payload> publisher =
            setup
                    .getRSocket()
                    .requestChannel(Flux.just(testPayload(0), testPayload(1), testPayload(2)));

    long count = publisher.count().block();

    assertThat(count).isEqualTo(3);
  }

  @Test
  @Timeout(10000)
  public void testChannel512() {
    Flux<Payload> payloads = Flux.range(1, 512).map(i -> DefaultPayload.create("hello " + i));

    long count = setup.getRSocket().requestChannel(payloads).count().block();

    assertThat(count).isEqualTo(512);
  }

  @Test
  @Timeout(30000)
  public void testChannel20_000() {
    Flux<Payload> payloads = Flux.range(1, 20_000).map(i -> DefaultPayload.create("hello " + i));

    long count = setup.getRSocket().requestChannel(payloads).count().block();

    assertThat(count).isEqualTo(20_000);
  }

  @Test
  @Timeout(60_000)
  public void testChannel200_000() {
    Flux<Payload> payloads = Flux.range(1, 200_000).map(i -> DefaultPayload.create("hello " + i));

    long count = setup.getRSocket().requestChannel(payloads).count().block();

    assertThat(count).isEqualTo(200_000);
  }

  @Test
  @Timeout(60_000)
  @Disabled
  public void testChannel2_000_000() {
    AtomicInteger counter = new AtomicInteger(0);

    Flux<Payload> payloads = Flux.range(1, 2_000_000).map(i -> DefaultPayload.create("hello " + i));
    long count = setup.getRSocket().requestChannel(payloads).count().block();

    assertThat(count).isEqualTo(2_000_000);
  }
}
