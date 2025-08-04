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
package infra.remoting.lb;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import infra.remoting.Payload;
import infra.remoting.Channel;
import infra.remoting.RaceTestConstants;
import infra.remoting.core.ChannelConnector;
import infra.remoting.internal.subscriber.AssertSubscriber;
import infra.remoting.test.util.TestClientTransport;
import infra.remoting.transport.ClientTransport;
import infra.remoting.util.EmptyPayload;
import infra.remoting.util.ChannelWrapper;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;
import reactor.test.util.RaceTestUtils;
import reactor.util.context.Context;

public class LoadbalanceTests {

  @BeforeEach
  void setUp() {
    Hooks.onErrorDropped((__) -> { });
  }

  @AfterAll
  static void afterAll() {
    Hooks.resetOnErrorDropped();
  }

  @Test
  public void shouldDeliverAllTheRequestsWithRoundRobinStrategy() {
    final AtomicInteger counter = new AtomicInteger();
    final ClientTransport mockTransport = new TestClientTransport();
    final Channel channel =
            new Channel() {
              @Override
              public Mono<Void> fireAndForget(Payload payload) {
                counter.incrementAndGet();
                return Mono.empty();
              }
            };

    final ChannelConnector channelConnectorMock = Mockito.mock(ChannelConnector.class);
    final ClientTransport mockTransport1 = Mockito.mock(ClientTransport.class);
    Mockito.when(channelConnectorMock.connect(Mockito.any(ClientTransport.class)))
            .then(im -> Mono.just(new TestChannel(channel)));

    final List<LoadBalanceTarget> collectionOfDestination1 =
            Collections.singletonList(LoadBalanceTarget.of("1", mockTransport));
    final List<LoadBalanceTarget> collectionOfDestination2 =
            Collections.singletonList(LoadBalanceTarget.of("2", mockTransport));
    final List<LoadBalanceTarget> collectionOfDestinations1And2 =
            Arrays.asList(
                    LoadBalanceTarget.of("1", mockTransport), LoadBalanceTarget.of("2", mockTransport));

    for (int i = 0; i < RaceTestConstants.REPEATS; i++) {
      final Sinks.Many<List<LoadBalanceTarget>> source =
              Sinks.unsafe().many().unicast().onBackpressureError();
      final ChannelPool channelPool =
              new ChannelPool(
                      channelConnectorMock, source.asFlux(), new RoundRobinLoadBalanceStrategy());
      final Mono<Void> fnfSource =
              Mono.defer(() -> channelPool.select().fireAndForget(EmptyPayload.INSTANCE));

      RaceTestUtils.race(
              () -> {
                for (int j = 0; j < 1000; j++) {
                  fnfSource.subscribe(new RetrySubscriber(fnfSource));
                }
              },
              () -> {
                for (int j = 0; j < 100; j++) {
                  source.emitNext(Collections.emptyList(), Sinks.EmitFailureHandler.FAIL_FAST);
                  source.emitNext(collectionOfDestination1, Sinks.EmitFailureHandler.FAIL_FAST);
                  source.emitNext(collectionOfDestinations1And2, Sinks.EmitFailureHandler.FAIL_FAST);
                  source.emitNext(collectionOfDestination1, Sinks.EmitFailureHandler.FAIL_FAST);
                  source.emitNext(collectionOfDestination2, Sinks.EmitFailureHandler.FAIL_FAST);
                  source.emitNext(Collections.emptyList(), Sinks.EmitFailureHandler.FAIL_FAST);
                  source.emitNext(collectionOfDestination2, Sinks.EmitFailureHandler.FAIL_FAST);
                  source.emitNext(collectionOfDestinations1And2, Sinks.EmitFailureHandler.FAIL_FAST);
                }
              });

      Assertions.assertThat(counter.get()).isEqualTo(1000);
      counter.set(0);
    }
  }

  @Test
  public void shouldDeliverAllTheRequestsWithWeightedStrategy() throws InterruptedException {
    final AtomicInteger counter = new AtomicInteger();

    final ClientTransport mockTransport1 = Mockito.mock(ClientTransport.class);
    final ClientTransport mockTransport2 = Mockito.mock(ClientTransport.class);

    final LoadBalanceTarget target1 = LoadBalanceTarget.of("1", mockTransport1);
    final LoadBalanceTarget target2 = LoadBalanceTarget.of("2", mockTransport2);

    final WeightedChannel weightedChannel1 = new WeightedChannel(counter);
    final WeightedChannel weightedChannel2 = new WeightedChannel(counter);

    final ChannelConnector channelConnectorMock = Mockito.mock(ChannelConnector.class);
    Mockito.when(channelConnectorMock.connect(mockTransport1))
            .then(im -> Mono.just(new TestChannel(weightedChannel1)));
    Mockito.when(channelConnectorMock.connect(mockTransport2))
            .then(im -> Mono.just(new TestChannel(weightedChannel2)));
    final List<LoadBalanceTarget> collectionOfDestination1 = Collections.singletonList(target1);
    final List<LoadBalanceTarget> collectionOfDestination2 = Collections.singletonList(target2);
    final List<LoadBalanceTarget> collectionOfDestinations1And2 = Arrays.asList(target1, target2);

    for (int i = 0; i < RaceTestConstants.REPEATS; i++) {
      final Sinks.Many<List<LoadBalanceTarget>> source =
              Sinks.unsafe().many().unicast().onBackpressureError();
      final ChannelPool channelPool =
              new ChannelPool(
                      channelConnectorMock,
                      source.asFlux(),
                      WeightedLoadBalanceStrategy.builder()
                              .weightedStatsResolver(
                                      channel -> {
                                        if (channel instanceof TestChannel) {
                                          return (WeightedChannel) ((TestChannel) channel).source();
                                        }
                                        return ((PooledChannel) channel).target() == target1
                                                ? weightedChannel1
                                                : weightedChannel2;
                                      })
                              .build());
      final Mono<Void> fnfSource =
              Mono.defer(() -> channelPool.select().fireAndForget(EmptyPayload.INSTANCE));

      RaceTestUtils.race(
              () -> {
                for (int j = 0; j < 1000; j++) {
                  fnfSource.subscribe(new RetrySubscriber(fnfSource));
                }
              },
              () -> {
                for (int j = 0; j < 100; j++) {
                  source.emitNext(Collections.emptyList(), Sinks.EmitFailureHandler.FAIL_FAST);
                  source.emitNext(collectionOfDestination1, Sinks.EmitFailureHandler.FAIL_FAST);
                  source.emitNext(collectionOfDestinations1And2, Sinks.EmitFailureHandler.FAIL_FAST);
                  source.emitNext(collectionOfDestination1, Sinks.EmitFailureHandler.FAIL_FAST);
                  source.emitNext(collectionOfDestination2, Sinks.EmitFailureHandler.FAIL_FAST);
                  source.emitNext(Collections.emptyList(), Sinks.EmitFailureHandler.FAIL_FAST);
                  source.emitNext(collectionOfDestination2, Sinks.EmitFailureHandler.FAIL_FAST);
                  source.emitNext(collectionOfDestinations1And2, Sinks.EmitFailureHandler.FAIL_FAST);
                }
              });

      Assertions.assertThat(counter.get()).isEqualTo(1000);
      counter.set(0);
    }
  }

  @Test
  public void ensureChannelIsCleanedFromThePoolIfSourceChannelIsDisposed() {
    final AtomicInteger counter = new AtomicInteger();
    final ClientTransport mockTransport = Mockito.mock(ClientTransport.class);
    final ChannelConnector channelConnectorMock = Mockito.mock(ChannelConnector.class);

    final TestChannel testChannel =
            new TestChannel(
                    new Channel() {
                      @Override
                      public Mono<Void> fireAndForget(Payload payload) {
                        counter.incrementAndGet();
                        return Mono.empty();
                      }
                    });

    Mockito.when(channelConnectorMock.connect(Mockito.any(ClientTransport.class)))
            .then(im -> Mono.delay(Duration.ofMillis(200)).map(__ -> testChannel));

    final TestPublisher<List<LoadBalanceTarget>> source = TestPublisher.create();
    final ChannelPool channelPool =
            new ChannelPool(channelConnectorMock, source, new RoundRobinLoadBalanceStrategy());

    source.next(Collections.singletonList(LoadBalanceTarget.of("1", mockTransport)));

    StepVerifier.create(channelPool.select().fireAndForget(EmptyPayload.INSTANCE))
            .expectSubscription()
            .expectComplete()
            .verify(Duration.ofSeconds(2));

    testChannel.dispose();

    Assertions.assertThatThrownBy(
                    () ->
                            channelPool
                                    .select()
                                    .fireAndForget(EmptyPayload.INSTANCE)
                                    .block(Duration.ofSeconds(2)))
            .isExactlyInstanceOf(IllegalStateException.class)
            .hasMessage("Timeout on blocking read for 2000000000 NANOSECONDS");

    Assertions.assertThat(counter.get()).isOne();
  }

  @Test
  public void ensureContextIsPropagatedCorrectlyForRequestChannel() {
    final AtomicInteger counter = new AtomicInteger();
    final ClientTransport mockTransport = Mockito.mock(ClientTransport.class);
    final ChannelConnector channelConnectorMock = Mockito.mock(ChannelConnector.class);

    Mockito.when(channelConnectorMock.connect(Mockito.any(ClientTransport.class)))
            .then(
                    im ->
                            Mono.delay(Duration.ofMillis(200))
                                    .map(
                                            __ ->
                                                    new TestChannel(
                                                            new Channel() {
                                                              @Override
                                                              public Flux<Payload> requestChannel(Publisher<Payload> source) {
                                                                counter.incrementAndGet();
                                                                return Flux.from(source);
                                                              }
                                                            })));

    final TestPublisher<List<LoadBalanceTarget>> source = TestPublisher.create();
    final ChannelPool channelPool =
            new ChannelPool(channelConnectorMock, source, new RoundRobinLoadBalanceStrategy());

    // check that context is propagated when there is no channel
    StepVerifier.create(
                    channelPool
                            .select()
                            .requestChannel(
                                    Flux.deferContextual(
                                            cv -> {
                                              if (cv.hasKey("test") && cv.get("test").equals("test")) {
                                                return Flux.just(EmptyPayload.INSTANCE);
                                              }
                                              else {
                                                return Flux.error(
                                                        new IllegalStateException("Expected context to be propagated"));
                                              }
                                            }))
                            .contextWrite(Context.of("test", "test")))
            .expectSubscription()
            .then(
                    () ->
                            source.next(Collections.singletonList(LoadBalanceTarget.of("1", mockTransport))))
            .expectNextCount(1)
            .expectComplete()
            .verify(Duration.ofSeconds(2));

    source.next(Collections.singletonList(LoadBalanceTarget.of("2", mockTransport)));
    // check that context is propagated when there is a Channel but it is unresolved
    StepVerifier.create(
                    channelPool
                            .select()
                            .requestChannel(
                                    Flux.deferContextual(
                                            cv -> {
                                              if (cv.hasKey("test") && cv.get("test").equals("test")) {
                                                return Flux.just(EmptyPayload.INSTANCE);
                                              }
                                              else {
                                                return Flux.error(
                                                        new IllegalStateException("Expected context to be propagated"));
                                              }
                                            }))
                            .contextWrite(Context.of("test", "test")))
            .expectSubscription()
            .expectNextCount(1)
            .expectComplete()
            .verify(Duration.ofSeconds(2));

    // check that context is propagated when there is a Channel and it is resolved
    StepVerifier.create(
                    channelPool
                            .select()
                            .requestChannel(
                                    Flux.deferContextual(
                                            cv -> {
                                              if (cv.hasKey("test") && cv.get("test").equals("test")) {
                                                return Flux.just(EmptyPayload.INSTANCE);
                                              }
                                              else {
                                                return Flux.error(
                                                        new IllegalStateException("Expected context to be propagated"));
                                              }
                                            }))
                            .contextWrite(Context.of("test", "test")))
            .expectSubscription()
            .expectNextCount(1)
            .expectComplete()
            .verify(Duration.ofSeconds(2));

    Assertions.assertThat(counter.get()).isEqualTo(3);
  }

  @Test
  public void shouldNotifyOnCloseWhenAllTheActiveSubscribersAreClosed() {
    final AtomicInteger counter = new AtomicInteger();
    final ClientTransport mockTransport = Mockito.mock(ClientTransport.class);
    final ChannelConnector channelConnectorMock = Mockito.mock(ChannelConnector.class);

    Sinks.Empty<Void> onCloseSocket1 = Sinks.empty();
    Sinks.Empty<Void> onCloseSocket2 = Sinks.empty();

    Channel socket1 =
            new Channel() {
              @Override
              public Mono<Void> onClose() {
                return onCloseSocket1.asMono();
              }

              @Override
              public Mono<Void> fireAndForget(Payload payload) {
                return Mono.empty();
              }
            };
    Channel socket2 =
            new Channel() {
              @Override
              public Mono<Void> onClose() {
                return onCloseSocket2.asMono();
              }

              @Override
              public Mono<Void> fireAndForget(Payload payload) {
                return Mono.empty();
              }
            };

    Mockito.when(channelConnectorMock.connect(Mockito.any(ClientTransport.class)))
            .then(im -> Mono.just(socket1))
            .then(im -> Mono.just(socket2))
            .then(im -> Mono.never().doOnCancel(() -> counter.incrementAndGet()));

    final TestPublisher<List<LoadBalanceTarget>> source = TestPublisher.create();
    final ChannelPool channelPool =
            new ChannelPool(channelConnectorMock, source, new RoundRobinLoadBalanceStrategy());

    source.next(
            Arrays.asList(
                    LoadBalanceTarget.of("1", mockTransport),
                    LoadBalanceTarget.of("2", mockTransport),
                    LoadBalanceTarget.of("3", mockTransport)));

    StepVerifier.create(channelPool.select().fireAndForget(EmptyPayload.INSTANCE))
            .expectSubscription()
            .expectComplete()
            .verify(Duration.ofSeconds(2));

    StepVerifier.create(channelPool.select().fireAndForget(EmptyPayload.INSTANCE))
            .expectSubscription()
            .expectComplete()
            .verify(Duration.ofSeconds(2));

    channelPool.select().fireAndForget(EmptyPayload.INSTANCE).subscribe();

    channelPool.dispose();

    AssertSubscriber<Void> onCloseSubscriber =
            channelPool.onClose().subscribeWith(AssertSubscriber.create());

    onCloseSubscriber.assertNotTerminated();

    onCloseSocket1.tryEmitEmpty();

    onCloseSubscriber.assertNotTerminated();

    onCloseSocket2.tryEmitEmpty();

    onCloseSubscriber.assertTerminated().assertComplete();

    Assertions.assertThat(counter.get()).isOne();
  }

  static class TestChannel extends ChannelWrapper {

    final Sinks.Empty<Void> sink = Sinks.empty();

    public TestChannel(Channel channel) {
      super(channel);
    }

    @Override
    public Mono<Void> onClose() {
      return sink.asMono();
    }

    @Override
    public void dispose() {
      sink.tryEmitEmpty();
    }

    public Channel source() {
      return delegate;
    }
  }

  private static class WeightedChannel extends BaseWeightedStats implements Channel {

    private final AtomicInteger counter;

    public WeightedChannel(AtomicInteger counter) {
      this.counter = counter;
    }

    @Override
    public Mono<Void> fireAndForget(Payload payload) {
      final long startTime = startRequest();
      counter.incrementAndGet();
      return Mono.<Void>empty()
              .doFinally(
                      (__) -> {
                        final long stopTime = stopRequest(startTime);
                        record(stopTime - startTime);
                      });
    }
  }

  static class RetrySubscriber implements CoreSubscriber<Void> {

    final Publisher<Void> source;

    private RetrySubscriber(Publisher<Void> source) {
      this.source = source;
    }

    @Override
    public void onSubscribe(Subscription s) {
      s.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(Void unused) { }

    @Override
    public void onError(Throwable t) {
      source.subscribe(this);
    }

    @Override
    public void onComplete() { }
  }
}
