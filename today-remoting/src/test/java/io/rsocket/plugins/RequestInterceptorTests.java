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

package io.rsocket.plugins;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import infra.lang.Nullable;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.Closeable;
import io.rsocket.Payload;
import io.rsocket.Channel;
import io.rsocket.ChannelAcceptor;
import io.rsocket.buffer.LeaksTrackingByteBufAllocator;
import io.rsocket.core.ChannelConnector;
import io.rsocket.core.RemotingServer;
import io.rsocket.frame.FrameType;
import infra.remoting.transport.local.LocalClientTransport;
import infra.remoting.transport.local.LocalServerTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

public class RequestInterceptorTests {

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void interceptorShouldBeInstalledProperlyOnTheClientRequesterSide(boolean errorOutcome) {
    final LeaksTrackingByteBufAllocator byteBufAllocator =
            LeaksTrackingByteBufAllocator.instrument(
                    ByteBufAllocator.DEFAULT, Duration.ofSeconds(1), "test");
    final Closeable closeable =
            RemotingServer.create(
                            ChannelAcceptor.with(
                                    new Channel() {
                                      @Override
                                      public Mono<Void> fireAndForget(Payload payload) {
                                        return Mono.empty();
                                      }

                                      @Override
                                      public Mono<Payload> requestResponse(Payload payload) {
                                        return errorOutcome
                                                ? Mono.error(new RuntimeException("test"))
                                                : Mono.just(payload);
                                      }

                                      @Override
                                      public Flux<Payload> requestStream(Payload payload) {
                                        return errorOutcome
                                                ? Flux.error(new RuntimeException("test"))
                                                : Flux.just(payload);
                                      }

                                      @Override
                                      public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
                                        return errorOutcome
                                                ? Flux.error(new RuntimeException("test"))
                                                : Flux.from(payloads);
                                      }
                                    }))
                    .bindNow(LocalServerTransport.create("test"));

    final TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();
    final Channel channel =
            ChannelConnector.create()
                    .interceptors(
                            ir ->
                                    ir.forRequestsInRequester(
                                            (Function<Channel, ? extends RequestInterceptor>)
                                                    (__) -> testRequestInterceptor))
                    .connect(LocalClientTransport.create("test", byteBufAllocator))
                    .block();

    try {
      channel
              .fireAndForget(DefaultPayload.create("test"))
              .onErrorResume(__ -> Mono.empty())
              .block();

      channel
              .requestResponse(DefaultPayload.create("test"))
              .onErrorResume(__ -> Mono.empty())
              .block();

      channel
              .requestStream(DefaultPayload.create("test"))
              .onErrorResume(__ -> Mono.empty())
              .blockLast();

      channel
              .requestChannel(Flux.just(DefaultPayload.create("test")))
              .onErrorResume(__ -> Mono.empty())
              .blockLast();

      testRequestInterceptor
              .expectOnStart(1, FrameType.REQUEST_FNF)
              .expectOnComplete(1)
              .expectOnStart(3, FrameType.REQUEST_RESPONSE)
              .assertNext(
                      e ->
                              assertThat(e)
                                      .hasFieldOrPropertyWithValue("streamId", 3)
                                      .hasFieldOrPropertyWithValue(
                                              "eventType",
                                              errorOutcome
                                                      ? TestRequestInterceptor.EventType.ON_ERROR
                                                      : TestRequestInterceptor.EventType.ON_COMPLETE))
              .expectOnStart(5, FrameType.REQUEST_STREAM)
              .assertNext(
                      e ->
                              assertThat(e)
                                      .hasFieldOrPropertyWithValue("streamId", 5)
                                      .hasFieldOrPropertyWithValue(
                                              "eventType",
                                              errorOutcome
                                                      ? TestRequestInterceptor.EventType.ON_ERROR
                                                      : TestRequestInterceptor.EventType.ON_COMPLETE))
              .expectOnStart(7, FrameType.REQUEST_CHANNEL)
              .assertNext(
                      e ->
                              assertThat(e)
                                      .hasFieldOrPropertyWithValue("streamId", 7)
                                      .hasFieldOrPropertyWithValue(
                                              "eventType",
                                              errorOutcome
                                                      ? TestRequestInterceptor.EventType.ON_ERROR
                                                      : TestRequestInterceptor.EventType.ON_COMPLETE))
              .expectNothing();
    }
    finally {
      channel.dispose();
      closeable.dispose();
      byteBufAllocator.assertHasNoLeaks();
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void interceptorShouldBeInstalledProperlyOnTheClientResponderSide(boolean errorOutcome)
          throws InterruptedException {
    final LeaksTrackingByteBufAllocator byteBufAllocator =
            LeaksTrackingByteBufAllocator.instrument(
                    ByteBufAllocator.DEFAULT, Duration.ofSeconds(1), "test");

    CountDownLatch latch = new CountDownLatch(1);
    final Closeable closeable =
            RemotingServer.create(
                            (setup, rSocket) ->
                                    Mono.<Channel>just(new Channel() { })
                                            .doAfterTerminate(
                                                    () -> {
                                                      new Thread(
                                                              () -> {
                                                                rSocket
                                                                        .fireAndForget(DefaultPayload.create("test"))
                                                                        .onErrorResume(__ -> Mono.empty())
                                                                        .block();

                                                                rSocket
                                                                        .requestResponse(DefaultPayload.create("test"))
                                                                        .onErrorResume(__ -> Mono.empty())
                                                                        .block();

                                                                rSocket
                                                                        .requestStream(DefaultPayload.create("test"))
                                                                        .onErrorResume(__ -> Mono.empty())
                                                                        .blockLast();

                                                                rSocket
                                                                        .requestChannel(
                                                                                Flux.just(DefaultPayload.create("test")))
                                                                        .onErrorResume(__ -> Mono.empty())
                                                                        .blockLast();
                                                                latch.countDown();
                                                              })
                                                              .start();
                                                    }))
                    .bindNow(LocalServerTransport.create("test"));

    final TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();
    final Channel channel =
            ChannelConnector.create()
                    .acceptor(
                            ChannelAcceptor.with(
                                    new Channel() {
                                      @Override
                                      public Mono<Void> fireAndForget(Payload payload) {
                                        return Mono.empty();
                                      }

                                      @Override
                                      public Mono<Payload> requestResponse(Payload payload) {
                                        return errorOutcome
                                                ? Mono.error(new RuntimeException("test"))
                                                : Mono.just(payload);
                                      }

                                      @Override
                                      public Flux<Payload> requestStream(Payload payload) {
                                        return errorOutcome
                                                ? Flux.error(new RuntimeException("test"))
                                                : Flux.just(payload);
                                      }

                                      @Override
                                      public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
                                        return errorOutcome
                                                ? Flux.error(new RuntimeException("test"))
                                                : Flux.from(payloads);
                                      }
                                    }))
                    .interceptors(
                            ir ->
                                    ir.forRequestsInResponder(
                                            (Function<Channel, ? extends RequestInterceptor>)
                                                    (__) -> testRequestInterceptor))
                    .connect(LocalClientTransport.create("test", byteBufAllocator))
                    .block();

    try {
      assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();

      testRequestInterceptor
              .expectOnStart(2, FrameType.REQUEST_FNF)
              .expectOnComplete(2)
              .expectOnStart(4, FrameType.REQUEST_RESPONSE)
              .assertNext(
                      e ->
                              assertThat(e)
                                      .hasFieldOrPropertyWithValue("streamId", 4)
                                      .hasFieldOrPropertyWithValue(
                                              "eventType",
                                              errorOutcome
                                                      ? TestRequestInterceptor.EventType.ON_ERROR
                                                      : TestRequestInterceptor.EventType.ON_COMPLETE))
              .expectOnStart(6, FrameType.REQUEST_STREAM)
              .assertNext(
                      e ->
                              assertThat(e)
                                      .hasFieldOrPropertyWithValue("streamId", 6)
                                      .hasFieldOrPropertyWithValue(
                                              "eventType",
                                              errorOutcome
                                                      ? TestRequestInterceptor.EventType.ON_ERROR
                                                      : TestRequestInterceptor.EventType.ON_COMPLETE))
              .expectOnStart(8, FrameType.REQUEST_CHANNEL)
              .assertNext(
                      e ->
                              assertThat(e)
                                      .hasFieldOrPropertyWithValue("streamId", 8)
                                      .hasFieldOrPropertyWithValue(
                                              "eventType",
                                              errorOutcome
                                                      ? TestRequestInterceptor.EventType.ON_ERROR
                                                      : TestRequestInterceptor.EventType.ON_COMPLETE))
              .expectNothing();

    }
    finally {
      channel.dispose();
      closeable.dispose();
      byteBufAllocator.assertHasNoLeaks();
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void interceptorShouldBeInstalledProperlyOnTheServerRequesterSide(boolean errorOutcome) {
    final LeaksTrackingByteBufAllocator byteBufAllocator =
            LeaksTrackingByteBufAllocator.instrument(
                    ByteBufAllocator.DEFAULT, Duration.ofSeconds(1), "test");

    final TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();
    final Closeable closeable =
            RemotingServer.create(
                            ChannelAcceptor.with(
                                    new Channel() {
                                      @Override
                                      public Mono<Void> fireAndForget(Payload payload) {
                                        return Mono.empty();
                                      }

                                      @Override
                                      public Mono<Payload> requestResponse(Payload payload) {
                                        return errorOutcome
                                                ? Mono.error(new RuntimeException("test"))
                                                : Mono.just(payload);
                                      }

                                      @Override
                                      public Flux<Payload> requestStream(Payload payload) {
                                        return errorOutcome
                                                ? Flux.error(new RuntimeException("test"))
                                                : Flux.just(payload);
                                      }

                                      @Override
                                      public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
                                        return errorOutcome
                                                ? Flux.error(new RuntimeException("test"))
                                                : Flux.from(payloads);
                                      }
                                    }))
                    .interceptors(
                            ir ->
                                    ir.forRequestsInResponder(
                                            (Function<Channel, ? extends RequestInterceptor>)
                                                    (__) -> testRequestInterceptor))
                    .bindNow(LocalServerTransport.create("test"));
    final Channel channel =
            ChannelConnector.create()
                    .connect(LocalClientTransport.create("test", byteBufAllocator))
                    .block();

    try {
      channel
              .fireAndForget(DefaultPayload.create("test"))
              .onErrorResume(__ -> Mono.empty())
              .block();

      channel
              .requestResponse(DefaultPayload.create("test"))
              .onErrorResume(__ -> Mono.empty())
              .block();

      channel
              .requestStream(DefaultPayload.create("test"))
              .onErrorResume(__ -> Mono.empty())
              .blockLast();

      channel
              .requestChannel(Flux.just(DefaultPayload.create("test")))
              .onErrorResume(__ -> Mono.empty())
              .blockLast();

      testRequestInterceptor
              .expectOnStart(1, FrameType.REQUEST_FNF)
              .expectOnComplete(1)
              .expectOnStart(3, FrameType.REQUEST_RESPONSE)
              .assertNext(
                      e ->
                              assertThat(e)
                                      .hasFieldOrPropertyWithValue("streamId", 3)
                                      .hasFieldOrPropertyWithValue(
                                              "eventType",
                                              errorOutcome
                                                      ? TestRequestInterceptor.EventType.ON_ERROR
                                                      : TestRequestInterceptor.EventType.ON_COMPLETE))
              .expectOnStart(5, FrameType.REQUEST_STREAM)
              .assertNext(
                      e ->
                              assertThat(e)
                                      .hasFieldOrPropertyWithValue("streamId", 5)
                                      .hasFieldOrPropertyWithValue(
                                              "eventType",
                                              errorOutcome
                                                      ? TestRequestInterceptor.EventType.ON_ERROR
                                                      : TestRequestInterceptor.EventType.ON_COMPLETE))
              .expectOnStart(7, FrameType.REQUEST_CHANNEL)
              .assertNext(
                      e ->
                              assertThat(e)
                                      .hasFieldOrPropertyWithValue("streamId", 7)
                                      .hasFieldOrPropertyWithValue(
                                              "eventType",
                                              errorOutcome
                                                      ? TestRequestInterceptor.EventType.ON_ERROR
                                                      : TestRequestInterceptor.EventType.ON_COMPLETE))
              .expectNothing();
    }
    finally {
      channel.dispose();
      closeable.dispose();
      byteBufAllocator.assertHasNoLeaks();
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void interceptorShouldBeInstalledProperlyOnTheServerResponderSide(boolean errorOutcome)
          throws InterruptedException {
    final LeaksTrackingByteBufAllocator byteBufAllocator =
            LeaksTrackingByteBufAllocator.instrument(
                    ByteBufAllocator.DEFAULT, Duration.ofSeconds(1), "test");

    CountDownLatch latch = new CountDownLatch(1);
    final TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();
    final Closeable closeable = RemotingServer.create((setup, rSocket) -> Mono.<Channel>just(new Channel() { })
                    .doAfterTerminate(() -> {
                      new Thread(() -> {
                        rSocket
                                .fireAndForget(DefaultPayload.create("test"))
                                .onErrorResume(__ -> Mono.empty())
                                .block();

                        rSocket
                                .requestResponse(DefaultPayload.create("test"))
                                .onErrorResume(__ -> Mono.empty())
                                .block();

                        rSocket
                                .requestStream(DefaultPayload.create("test"))
                                .onErrorResume(__ -> Mono.empty())
                                .blockLast();

                        rSocket
                                .requestChannel(
                                        Flux.just(DefaultPayload.create("test")))
                                .onErrorResume(__ -> Mono.empty())
                                .blockLast();
                        latch.countDown();
                      })
                              .start();
                    }))
            .interceptors(ir -> ir.forRequestsInRequester((Function<Channel, ? extends RequestInterceptor>)
                    (__) -> testRequestInterceptor))
            .bindNow(LocalServerTransport.create("test"));
    final Channel channel = ChannelConnector.create()
            .acceptor(ChannelAcceptor.with(new Channel() {
              @Override
              public Mono<Void> fireAndForget(Payload payload) {
                return errorOutcome
                        ? Mono.error(new RuntimeException("test"))
                        : Mono.empty();
              }

              @Override
              public Mono<Payload> requestResponse(Payload payload) {
                return errorOutcome
                        ? Mono.error(new RuntimeException("test"))
                        : Mono.just(payload);
              }

              @Override
              public Flux<Payload> requestStream(Payload payload) {
                return errorOutcome
                        ? Flux.error(new RuntimeException("test"))
                        : Flux.just(payload);
              }

              @Override
              public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
                return errorOutcome
                        ? Flux.error(new RuntimeException("test"))
                        : Flux.from(payloads);
              }
            }))
            .connect(LocalClientTransport.create("test", byteBufAllocator))
            .block();

    try {
      assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();

      testRequestInterceptor
              .expectOnStart(2, FrameType.REQUEST_FNF)
              .expectOnComplete(2)
              .expectOnStart(4, FrameType.REQUEST_RESPONSE)
              .assertNext(e -> assertThat(e)
                      .hasFieldOrPropertyWithValue("streamId", 4)
                      .hasFieldOrPropertyWithValue("eventType", errorOutcome
                              ? TestRequestInterceptor.EventType.ON_ERROR
                              : TestRequestInterceptor.EventType.ON_COMPLETE))
              .expectOnStart(6, FrameType.REQUEST_STREAM)
              .assertNext(e -> assertThat(e)
                      .hasFieldOrPropertyWithValue("streamId", 6)
                      .hasFieldOrPropertyWithValue(
                              "eventType",
                              errorOutcome
                                      ? TestRequestInterceptor.EventType.ON_ERROR
                                      : TestRequestInterceptor.EventType.ON_COMPLETE))
              .expectOnStart(8, FrameType.REQUEST_CHANNEL)
              .assertNext(e -> assertThat(e)
                      .hasFieldOrPropertyWithValue("streamId", 8)
                      .hasFieldOrPropertyWithValue("eventType", errorOutcome
                              ? TestRequestInterceptor.EventType.ON_ERROR
                              : TestRequestInterceptor.EventType.ON_COMPLETE))
              .expectNothing();

    }
    finally {
      channel.dispose();
      closeable.dispose();
      byteBufAllocator.assertHasNoLeaks();
    }
  }

  @Test
  void ensuresExceptionInTheInterceptorIsHandledProperly() {
    final LeaksTrackingByteBufAllocator byteBufAllocator = LeaksTrackingByteBufAllocator.instrument(ByteBufAllocator.DEFAULT, Duration.ofSeconds(1), "test");

    final Closeable closeable = RemotingServer.create(ChannelAcceptor.with(
                    new Channel() {
                      @Override
                      public Mono<Void> fireAndForget(Payload payload) {
                        return Mono.empty();
                      }

                      @Override
                      public Mono<Payload> requestResponse(Payload payload) {
                        return Mono.just(payload);
                      }

                      @Override
                      public Flux<Payload> requestStream(Payload payload) {
                        return Flux.just(payload);
                      }

                      @Override
                      public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
                        return Flux.from(payloads);
                      }
                    }))
            .bindNow(LocalServerTransport.create("test"));

    final RequestInterceptor testRequestInterceptor = new RequestInterceptor() {
      @Override
      public void onStart(int streamId, FrameType requestType, @Nullable ByteBuf metadata) {
        throw new RuntimeException("testOnStart");
      }

      @Override
      public void onTerminate(
              int streamId, FrameType requestType, @Nullable Throwable terminalSignal) {
        throw new RuntimeException("testOnTerminate");
      }

      @Override
      public void onCancel(int streamId, FrameType requestType) {
        throw new RuntimeException("testOnCancel");
      }

      @Override
      public void onReject(
              Throwable rejectionReason, FrameType requestType, @Nullable ByteBuf metadata) {
        throw new RuntimeException("testOnReject");
      }

      @Override
      public void dispose() { }
    };
    final Channel channel = ChannelConnector.create()
            .interceptors(ir -> ir.forRequestsInRequester((Function<Channel, ? extends RequestInterceptor>) (__) -> testRequestInterceptor))
            .connect(LocalClientTransport.create("test", byteBufAllocator))
            .block();

    try {
      StepVerifier.create(channel.fireAndForget(DefaultPayload.create("test")))
              .expectSubscription()
              .expectComplete()
              .verify();

      StepVerifier.create(channel.requestResponse(DefaultPayload.create("test")))
              .expectSubscription()
              .expectNextCount(1)
              .expectComplete()
              .verify();

      StepVerifier.create(channel.requestStream(DefaultPayload.create("test")))
              .expectSubscription()
              .expectNextCount(1)
              .expectComplete()
              .verify();

      StepVerifier.create(channel.requestChannel(Flux.just(DefaultPayload.create("test"))))
              .expectSubscription()
              .expectNextCount(1)
              .expectComplete()
              .verify();
    }
    finally {
      channel.dispose();
      closeable.dispose();
      byteBufAllocator.assertHasNoLeaks();
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void shouldSupportMultipleInterceptors(boolean errorOutcome) {
    final LeaksTrackingByteBufAllocator byteBufAllocator =
            LeaksTrackingByteBufAllocator.instrument(
                    ByteBufAllocator.DEFAULT, Duration.ofSeconds(1), "test");

    final Closeable closeable = RemotingServer.create(ChannelAcceptor.with(
                    new Channel() {
                      @Override
                      public Mono<Void> fireAndForget(Payload payload) {
                        return Mono.empty();
                      }

                      @Override
                      public Mono<Payload> requestResponse(Payload payload) {
                        return errorOutcome
                                ? Mono.error(new RuntimeException("test"))
                                : Mono.just(payload);
                      }

                      @Override
                      public Flux<Payload> requestStream(Payload payload) {
                        return errorOutcome
                                ? Flux.error(new RuntimeException("test"))
                                : Flux.just(payload);
                      }

                      @Override
                      public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
                        return errorOutcome
                                ? Flux.error(new RuntimeException("test"))
                                : Flux.from(payloads);
                      }
                    }))
            .bindNow(LocalServerTransport.create("test"));

    final RequestInterceptor testRequestInterceptor1 = new RequestInterceptor() {
      @Override
      public void onStart(int streamId, FrameType requestType, @Nullable ByteBuf metadata) {
        throw new RuntimeException("testOnStart");
      }

      @Override
      public void onTerminate(
              int streamId, FrameType requestType, @Nullable Throwable terminalSignal) {
        throw new RuntimeException("testOnTerminate");
      }

      @Override
      public void onCancel(int streamId, FrameType requestType) {
        throw new RuntimeException("testOnTerminate");
      }

      @Override
      public void onReject(
              Throwable rejectionReason, FrameType requestType, @Nullable ByteBuf metadata) {
        throw new RuntimeException("testOnReject");
      }

      @Override
      public void dispose() { }
    };
    final TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();
    final TestRequestInterceptor testRequestInterceptor2 = new TestRequestInterceptor();
    final Channel channel = ChannelConnector.create().interceptors(ir ->
                    ir.forRequestsInRequester((Function<Channel, ? extends RequestInterceptor>) (__) -> testRequestInterceptor)
                            .forRequestsInRequester((Function<Channel, ? extends RequestInterceptor>) (__) -> testRequestInterceptor1)
                            .forRequestsInRequester((Function<Channel, ? extends RequestInterceptor>) (__) -> testRequestInterceptor2))
            .connect(LocalClientTransport.create("test", byteBufAllocator))
            .block();

    try {
      channel
              .fireAndForget(DefaultPayload.create("test"))
              .onErrorResume(__ -> Mono.empty())
              .block();

      channel
              .requestResponse(DefaultPayload.create("test"))
              .onErrorResume(__ -> Mono.empty())
              .block();

      channel
              .requestStream(DefaultPayload.create("test"))
              .onErrorResume(__ -> Mono.empty())
              .blockLast();

      channel
              .requestChannel(Flux.just(DefaultPayload.create("test")))
              .onErrorResume(__ -> Mono.empty())
              .blockLast();

      testRequestInterceptor
              .expectOnStart(1, FrameType.REQUEST_FNF)
              .expectOnComplete(1)
              .expectOnStart(3, FrameType.REQUEST_RESPONSE)
              .assertNext(e -> assertThat(e)
                      .hasFieldOrPropertyWithValue("streamId", 3)
                      .hasFieldOrPropertyWithValue("eventType", errorOutcome
                              ? TestRequestInterceptor.EventType.ON_ERROR
                              : TestRequestInterceptor.EventType.ON_COMPLETE))
              .expectOnStart(5, FrameType.REQUEST_STREAM)
              .assertNext(e -> assertThat(e)
                      .hasFieldOrPropertyWithValue("streamId", 5)
                      .hasFieldOrPropertyWithValue("eventType", errorOutcome
                              ? TestRequestInterceptor.EventType.ON_ERROR
                              : TestRequestInterceptor.EventType.ON_COMPLETE))
              .expectOnStart(7, FrameType.REQUEST_CHANNEL)
              .assertNext(e -> assertThat(e)
                      .hasFieldOrPropertyWithValue("streamId", 7)
                      .hasFieldOrPropertyWithValue("eventType", errorOutcome
                              ? TestRequestInterceptor.EventType.ON_ERROR
                              : TestRequestInterceptor.EventType.ON_COMPLETE))
              .expectNothing();

      testRequestInterceptor2
              .expectOnStart(1, FrameType.REQUEST_FNF)
              .expectOnComplete(1)
              .expectOnStart(3, FrameType.REQUEST_RESPONSE)
              .assertNext(e -> assertThat(e)
                      .hasFieldOrPropertyWithValue("streamId", 3)
                      .hasFieldOrPropertyWithValue("eventType", errorOutcome
                              ? TestRequestInterceptor.EventType.ON_ERROR
                              : TestRequestInterceptor.EventType.ON_COMPLETE))
              .expectOnStart(5, FrameType.REQUEST_STREAM)
              .assertNext(e -> assertThat(e)
                      .hasFieldOrPropertyWithValue("streamId", 5)
                      .hasFieldOrPropertyWithValue("eventType", errorOutcome
                              ? TestRequestInterceptor.EventType.ON_ERROR
                              : TestRequestInterceptor.EventType.ON_COMPLETE))
              .expectOnStart(7, FrameType.REQUEST_CHANNEL)
              .assertNext(e -> assertThat(e)
                      .hasFieldOrPropertyWithValue("streamId", 7)
                      .hasFieldOrPropertyWithValue("eventType", errorOutcome
                              ? TestRequestInterceptor.EventType.ON_ERROR
                              : TestRequestInterceptor.EventType.ON_COMPLETE))
              .expectNothing();
    }
    finally {
      channel.dispose();
      closeable.dispose();
      byteBufAllocator.assertHasNoLeaks();
    }
  }
}
