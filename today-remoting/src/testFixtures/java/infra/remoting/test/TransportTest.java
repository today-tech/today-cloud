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

package infra.remoting.test;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Assumptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import io.netty.util.ReferenceCounted;
import infra.remoting.Payload;
import infra.remoting.Channel;
import infra.remoting.util.ByteBufPayload;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.Logger;
import reactor.util.Loggers;

public interface TransportTest {

  Logger logger = Loggers.getLogger(TransportTest.class);

  String MOCK_DATA = "test-data";
  String MOCK_METADATA = "metadata";
  String LARGE_DATA = read("words.shakespeare.txt.gz");
  Payload LARGE_PAYLOAD = ByteBufPayload.create(LARGE_DATA, LARGE_DATA);

  static String read(String resourceName) {
    try (BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(
            TransportTest.class.getClassLoader().getResourceAsStream(resourceName))))) {

      return br.lines().map(String::toLowerCase).collect(Collectors.joining("\n\r"));
    }
    catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  @BeforeEach
  default void setup() {
    Hooks.onOperatorDebug();
  }

  @AfterEach
  default void close() {
    try {
      logger.debug("------------------Awaiting communication to finish------------------");
      getTransportPair().responder.awaitAllInteractionTermination(getTimeout());
      logger.debug("---------------------Disposing Client And Server--------------------");
      getTransportPair().dispose();
      getTransportPair().awaitClosed(getTimeout());
      logger.debug("------------------------Disposing Schedulers-------------------------");
      Schedulers.parallel().disposeGracefully().timeout(getTimeout(), Mono.empty()).block();
      Schedulers.boundedElastic().disposeGracefully().timeout(getTimeout(), Mono.empty()).block();
      Schedulers.single().disposeGracefully().timeout(getTimeout(), Mono.empty()).block();
      logger.debug("---------------------------Leaks Checking----------------------------");
      RuntimeException throwable =
              new RuntimeException() {
                @Override
                public synchronized Throwable fillInStackTrace() {
                  return this;
                }

                @Override
                public String getMessage() {
                  return Arrays.toString(getSuppressed());
                }
              };

      try {
        getTransportPair().byteBufAllocator2.assertHasNoLeaks();
      }
      catch (Throwable t) {
        throwable = Exceptions.addSuppressed(throwable, t);
      }

      try {
        getTransportPair().byteBufAllocator1.assertHasNoLeaks();
      }
      catch (Throwable t) {
        throwable = Exceptions.addSuppressed(throwable, t);
      }

      if (throwable.getSuppressed().length > 0) {
        throw throwable;
      }
    }
    finally {
      Hooks.resetOnOperatorDebug();
      Schedulers.resetOnHandleError();
    }
  }

  default Payload createTestPayload(int metadataPresent) {
    String metadata1;

    switch (metadataPresent % 5) {
      case 0:
        metadata1 = null;
        break;
      case 1:
        metadata1 = "";
        break;
      default:
        metadata1 = MOCK_METADATA;
        break;
    }
    String metadata = metadata1;

    return ByteBufPayload.create(MOCK_DATA, metadata);
  }

  @DisplayName("makes 10 fireAndForget requests")
  @Test
  default void fireAndForget10() {
    Flux.range(1, 10)
            .flatMap(i -> getClient().fireAndForget(createTestPayload(i)))
            .as(StepVerifier::create)
            .expectComplete()
            .verify(getTimeout());

    getTransportPair().responder.awaitUntilObserved(10, getTimeout());
  }

  @DisplayName("makes 10 fireAndForget with Large Payload in Requests")
  @Test
  default void largePayloadFireAndForget10() {
    Flux.range(1, 10)
            .flatMap(i -> getClient().fireAndForget(LARGE_PAYLOAD.retain()))
            .as(StepVerifier::create)
            .expectComplete()
            .verify(getTimeout());

    getTransportPair().responder.awaitUntilObserved(10, getTimeout());
  }

  default Channel getClient() {
    return getTransportPair().getClient();
  }

  Duration getTimeout();

  TransportPair getTransportPair();

  @DisplayName("makes 10 metadataPush requests")
  @Test
  default void metadataPush10() {
    Assumptions.assumeThat(getTransportPair().withResumability).isFalse();
    Flux.range(1, 10)
            .flatMap(i -> getClient().metadataPush(ByteBufPayload.create("", "test-metadata")))
            .as(StepVerifier::create)
            .expectComplete()
            .verify(getTimeout());

    getTransportPair().responder.awaitUntilObserved(10, getTimeout());
  }

  @DisplayName("makes 10 metadataPush with Large Metadata in requests")
  @Test
  default void largePayloadMetadataPush10() {
    Assumptions.assumeThat(getTransportPair().withResumability).isFalse();
    Flux.range(1, 10)
            .flatMap(i -> getClient().metadataPush(ByteBufPayload.create("", LARGE_DATA)))
            .as(StepVerifier::create)
            .expectComplete()
            .verify(getTimeout());

    getTransportPair().responder.awaitUntilObserved(10, getTimeout());
  }

  @DisplayName("makes 1 requestChannel request with 0 payloads")
  @Test
  default void requestChannel0() {
    getClient()
            .requestChannel(Flux.empty())
            .as(StepVerifier::create)
            .expectErrorSatisfies(
                    t ->
                            Assertions.assertThat(t)
                                    .isInstanceOf(CancellationException.class)
                                    .hasMessage("Empty Source"))
            .verify(getTimeout());
  }

  @DisplayName("makes 1 requestChannel request with 1 payloads")
  @Test
  default void requestChannel1() {
    getClient()
            .requestChannel(Mono.just(createTestPayload(0)))
            .doOnNext(Payload::release)
            .as(StepVerifier::create)
            .thenConsumeWhile(new PayloadPredicate(1))
            .expectComplete()
            .verify(getTimeout());
  }

  @DisplayName("makes 1 requestChannel request with 200,000 payloads")
  @Test
  default void requestChannel200_000() {
    Flux<Payload> payloads = Flux.range(0, 200_000).map(this::createTestPayload);

    getClient()
            .requestChannel(payloads)
            .doOnNext(Payload::release)
            .limitRate(8)
            .as(StepVerifier::create)
            .thenConsumeWhile(new PayloadPredicate(200_000))
            .expectComplete()
            .verify(getTimeout());
  }

  @DisplayName("makes 1 requestChannel request with 50 large payloads")
  @Test
  default void largePayloadRequestChannel50() {
    Flux<Payload> payloads = Flux.range(0, 50).map(__ -> LARGE_PAYLOAD.retain());

    getClient()
            .requestChannel(payloads)
            .doOnNext(Payload::release)
            .as(StepVerifier::create)
            .thenConsumeWhile(new PayloadPredicate(50))
            .expectComplete()
            .verify(getTimeout());
  }

  @DisplayName("makes 1 requestChannel request with 20,000 payloads")
  @Test
  default void requestChannel20_000() {
    Flux<Payload> payloads = Flux.range(0, 20_000).map(metadataPresent -> createTestPayload(7));

    getClient()
            .requestChannel(payloads)
            .doOnNext(this::assertChannelPayload)
            .doOnNext(Payload::release)
            .as(StepVerifier::create)
            .thenConsumeWhile(new PayloadPredicate(20_000))
            .expectComplete()
            .verify(getTimeout());
  }

  @DisplayName("makes 1 requestChannel request with 2,000,000 payloads")
  @SlowTest
  default void requestChannel2_000_000() {
    Flux<Payload> payloads = Flux.range(0, 2_000_000).map(this::createTestPayload);

    getClient()
            .requestChannel(payloads)
            .doOnNext(Payload::release)
            .limitRate(8)
            .as(StepVerifier::create)
            .thenConsumeWhile(new PayloadPredicate(2_000_000))
            .expectComplete()
            .verify(getTimeout());
  }

  @DisplayName("makes 1 requestChannel request with 3 payloads")
  @Test
  default void requestChannel3() {
    AtomicLong requested = new AtomicLong();
    Flux<Payload> payloads =
            Flux.range(0, 3).doOnRequest(requested::addAndGet).map(this::createTestPayload);

    getClient()
            .requestChannel(payloads)
            .doOnNext(Payload::release)
            .as(publisher -> StepVerifier.create(publisher, 3))
            .thenConsumeWhile(new PayloadPredicate(3))
            .expectComplete()
            .verify(getTimeout());

    Assertions.assertThat(requested.get()).isEqualTo(3L);
  }

  @DisplayName("makes 1 requestChannel request with 256 payloads")
  @Test
  default void requestChannel256() {
    AtomicInteger counter = new AtomicInteger();
    Flux<Payload> payloads = Flux.defer(() -> {
      final int subscription = counter.getAndIncrement();
      return Flux.range(0, 256)
              .map(i -> "S{" + subscription + "}: Data{" + i + "}")
              .map(ByteBufPayload::create);
    });
    final Scheduler scheduler = Schedulers.fromExecutorService(Executors.newFixedThreadPool(12));

    try {
      Flux.range(0, 1024)
              .flatMap(v -> Mono.fromRunnable(() -> check(payloads)).subscribeOn(scheduler), 12)
              .blockLast();
    }
    finally {
      scheduler.disposeGracefully().block();
    }
  }

  default void check(Flux<Payload> payloads) {
    getClient()
            .requestChannel(payloads)
            .doOnNext(ReferenceCounted::release)
            .limitRate(8)
            .as(StepVerifier::create)
            .thenConsumeWhile(new PayloadPredicate(256))
            .as("expected 256 items")
            .expectComplete()
            .verify(getTimeout());
  }

  @DisplayName("makes 1 requestResponse request")
  @Test
  default void requestResponse1() {
    getClient()
            .requestResponse(createTestPayload(1))
            .doOnNext(this::assertPayload)
            .doOnNext(Payload::release)
            .as(StepVerifier::create)
            .expectNextCount(1)
            .expectComplete()
            .verify(getTimeout());
  }

  @DisplayName("makes 10 requestResponse requests")
  @Test
  default void requestResponse10() {
    Flux.range(1, 10)
            .flatMap(
                    i ->
                            getClient()
                                    .requestResponse(createTestPayload(i))
                                    .doOnNext(v -> assertPayload(v))
                                    .doOnNext(Payload::release))
            .as(StepVerifier::create)
            .expectNextCount(10)
            .expectComplete()
            .verify(getTimeout());
  }

  @DisplayName("makes 100 requestResponse requests")
  @Test
  default void requestResponse100() {
    Flux.range(1, 100)
            .flatMap(i -> getClient().requestResponse(createTestPayload(i)).doOnNext(Payload::release))
            .as(StepVerifier::create)
            .expectNextCount(100)
            .expectComplete()
            .verify(getTimeout());
  }

  @DisplayName("makes 50 requestResponse requests")
  @Test
  default void largePayloadRequestResponse50() {
    Flux.range(1, 50)
            .flatMap(i -> getClient().requestResponse(LARGE_PAYLOAD.retain()).doOnNext(Payload::release))
            .as(StepVerifier::create)
            .expectNextCount(50)
            .expectComplete()
            .verify(getTimeout());
  }

  @DisplayName("makes 10,000 requestResponse requests")
  @Test
  default void requestResponse10_000() {
    Flux.range(1, 10_000)
            .flatMap(i -> getClient().requestResponse(createTestPayload(i)).doOnNext(Payload::release))
            .as(StepVerifier::create)
            .expectNextCount(10_000)
            .expectComplete()
            .verify(getTimeout());
  }

  @DisplayName("makes 1 requestStream request and receives 10,000 responses")
  @Test
  default void requestStream10_000() {
    getClient()
            .requestStream(createTestPayload(3))
            .doOnNext(this::assertPayload)
            .doOnNext(Payload::release)
            .as(StepVerifier::create)
            .expectNextCount(10_000)
            .expectComplete()
            .verify(getTimeout());
  }

  @DisplayName("makes 1 requestStream request and receives 5 responses")
  @Test
  default void requestStream5() {
    getClient()
            .requestStream(createTestPayload(3))
            .doOnNext(this::assertPayload)
            .doOnNext(Payload::release)
            .take(5)
            .as(StepVerifier::create)
            .expectNextCount(5)
            .expectComplete()
            .verify(getTimeout());
  }

  @DisplayName("makes 1 requestStream request and consumes result incrementally")
  @Test
  default void requestStreamDelayedRequestN() {
    getClient()
            .requestStream(createTestPayload(3))
            .take(10)
            .doOnNext(Payload::release)
            .as(StepVerifier::create)
            .thenRequest(5)
            .expectNextCount(5)
            .thenRequest(5)
            .expectNextCount(5)
            .expectComplete()
            .verify(getTimeout());
  }

  default void assertPayload(Payload p) {
    TransportPair transportPair = getTransportPair();
    if (!transportPair.expectedPayloadData().equals(p.getDataUtf8())
            || !transportPair.expectedPayloadMetadata().equals(p.getMetadataUtf8())) {
      throw new IllegalStateException("Unexpected payload");
    }
  }

  default void assertChannelPayload(Payload p) {
    if (!MOCK_DATA.equals(p.getDataUtf8()) || !MOCK_METADATA.equals(p.getMetadataUtf8())) {
      throw new IllegalStateException("Unexpected payload");
    }
  }

}
