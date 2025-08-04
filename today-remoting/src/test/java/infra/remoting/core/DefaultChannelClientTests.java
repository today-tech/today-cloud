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

package infra.remoting.core;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Assumptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import infra.remoting.FrameAssert;
import infra.remoting.Payload;
import infra.remoting.Channel;
import infra.remoting.RaceTestConstants;
import infra.remoting.frame.ErrorFrameCodec;
import infra.remoting.frame.FrameHeaderCodec;
import infra.remoting.frame.FrameType;
import infra.remoting.frame.PayloadFrameCodec;
import infra.remoting.frame.decoder.PayloadDecoder;
import infra.remoting.internal.subscriber.AssertSubscriber;
import infra.remoting.test.util.TestDuplexConnection;
import infra.remoting.util.ByteBufPayload;
import infra.remoting.util.ChannelWrapper;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;
import reactor.test.util.RaceTestUtils;
import reactor.util.context.Context;
import reactor.util.context.ContextView;
import reactor.util.retry.Retry;

public class DefaultChannelClientTests {

  ClientChannelRule rule;

  @BeforeEach
  public void setUp() throws Throwable {
    Hooks.onNextDropped(ReferenceCountUtil::safeRelease);
    Hooks.onErrorDropped((t) -> { });
    rule = new ClientChannelRule();
    rule.init();
  }

  @AfterEach
  public void tearDown() {
    Hooks.resetOnErrorDropped();
    Hooks.resetOnNextDropped();
    rule.allocator.assertHasNoLeaks();
  }

  @Test
  @SuppressWarnings("unchecked")
  void discardElementsConsumerShouldAcceptOtherTypesThanReferenceCounted() {
    Consumer discardElementsConsumer = DefaultRemotingClient.DISCARD_ELEMENTS_CONSUMER;
    discardElementsConsumer.accept(new Object());
  }

  @Test
  void droppedElementsConsumerReleaseReference() {
    ReferenceCounted referenceCounted = Mockito.mock(ReferenceCounted.class);
    Mockito.when(referenceCounted.release()).thenReturn(true);
    Mockito.when(referenceCounted.refCnt()).thenReturn(1);

    Consumer discardElementsConsumer = DefaultRemotingClient.DISCARD_ELEMENTS_CONSUMER;
    discardElementsConsumer.accept(referenceCounted);

    Mockito.verify(referenceCounted).release();
  }

  static Stream<Arguments> interactions() {
    return Stream.of(
            Arguments.of(
                    (BiFunction<RemotingClient, Publisher<Payload>, Publisher<?>>)
                            (client, payload) -> client.fireAndForget(Mono.fromDirect(payload)),
                    FrameType.REQUEST_FNF),
            Arguments.of(
                    (BiFunction<RemotingClient, Publisher<Payload>, Publisher<?>>)
                            (client, payload) -> client.requestResponse(Mono.fromDirect(payload)),
                    FrameType.REQUEST_RESPONSE),
            Arguments.of(
                    (BiFunction<RemotingClient, Publisher<Payload>, Publisher<?>>)
                            (client, payload) -> client.requestStream(Mono.fromDirect(payload)),
                    FrameType.REQUEST_STREAM),
            Arguments.of(
                    (BiFunction<RemotingClient, Publisher<Payload>, Publisher<?>>)
                            RemotingClient::requestChannel,
                    FrameType.REQUEST_CHANNEL),
            Arguments.of(
                    (BiFunction<RemotingClient, Publisher<Payload>, Publisher<?>>)
                            (client, payload) -> client.metadataPush(Mono.fromDirect(payload)),
                    FrameType.METADATA_PUSH));
  }

  @ParameterizedTest
  @MethodSource("interactions")
  public void shouldSentFrameOnResolution(
          BiFunction<RemotingClient, Publisher<Payload>, Publisher<?>> request, FrameType requestType) {
    Payload payload = ByteBufPayload.create("test", "testMetadata");
    TestPublisher<Payload> testPublisher =
            TestPublisher.createNoncompliant(TestPublisher.Violation.DEFER_CANCELLATION);

    Publisher<?> publisher = request.apply(rule.client, testPublisher);

    StepVerifier.create(publisher)
            .expectSubscription()
            .then(() -> Assertions.assertThat(rule.connection.getSent()).isEmpty())
            .then(
                    () -> {
                      if (requestType != FrameType.REQUEST_CHANNEL) {
                        testPublisher.next(payload);
                      }
                    })
            .then(() -> rule.delayer.run())
            .then(
                    () -> {
                      if (requestType == FrameType.REQUEST_CHANNEL) {
                        testPublisher.next(payload);
                      }
                    })
            .then(testPublisher::complete)
            .then(
                    () -> {
                      if (requestType == FrameType.REQUEST_CHANNEL) {
                        Assertions.assertThat(rule.connection.getSent())
                                .hasSize(2)
                                .first()
                                .matches(bb -> FrameHeaderCodec.frameType(bb).equals(requestType))
                                .matches(ReferenceCounted::release);

                        Assertions.assertThat(rule.connection.getSent())
                                .element(1)
                                .matches(bb -> FrameHeaderCodec.frameType(bb).equals(FrameType.COMPLETE))
                                .matches(ReferenceCounted::release);
                      }
                      else {
                        Assertions.assertThat(rule.connection.getSent())
                                .hasSize(1)
                                .first()
                                .matches(bb -> FrameHeaderCodec.frameType(bb).equals(requestType))
                                .matches(ReferenceCounted::release);
                      }
                    })
            .then(
                    () -> {
                      if (requestType != FrameType.REQUEST_FNF && requestType != FrameType.METADATA_PUSH) {
                        rule.connection.addToReceivedBuffer(
                                PayloadFrameCodec.encodeComplete(rule.allocator, 1));
                      }
                    })
            .expectComplete()
            .verify(Duration.ofMillis(1000));

    rule.allocator.assertHasNoLeaks();
  }

  @ParameterizedTest
  @MethodSource("interactions")
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void shouldHaveNoLeaksOnPayloadInCaseOfRacingOfOnNextAndCancel(
          BiFunction<RemotingClient, Publisher<Payload>, Publisher<?>> request, FrameType requestType) {
    Assumptions.assumeThat(requestType).isNotEqualTo(FrameType.REQUEST_CHANNEL);

    for (int i = 0; i < RaceTestConstants.REPEATS; i++) {
      ClientChannelRule rule = new ClientChannelRule();
      rule.init();
      Payload payload = ByteBufPayload.create("test", "testMetadata");
      TestPublisher<Payload> testPublisher =
              TestPublisher.createNoncompliant(TestPublisher.Violation.DEFER_CANCELLATION);
      AssertSubscriber assertSubscriber = AssertSubscriber.create(0);

      Publisher<?> publisher = request.apply(rule.client, testPublisher);
      publisher.subscribe(assertSubscriber);

      testPublisher.assertWasNotRequested();

      assertSubscriber.request(1);

      testPublisher.assertWasRequested();
      testPublisher.assertMaxRequested(1);
      testPublisher.assertMinRequested(1);

      RaceTestUtils.race(
              () -> {
                testPublisher.next(payload);
                rule.delayer.run();
              },
              assertSubscriber::cancel);

      Collection<ByteBuf> sent = rule.connection.getSent();
      if (sent.size() == 1) {
        Assertions.assertThat(sent)
                .allMatch(bb -> FrameHeaderCodec.frameType(bb).equals(requestType))
                .allMatch(ReferenceCounted::release);
      }
      else if (sent.size() == 2) {
        Assertions.assertThat(sent)
                .first()
                .matches(bb -> FrameHeaderCodec.frameType(bb).equals(requestType))
                .matches(ReferenceCounted::release);
        Assertions.assertThat(sent)
                .element(1)
                .matches(bb -> FrameHeaderCodec.frameType(bb).equals(FrameType.CANCEL))
                .matches(ReferenceCounted::release);
      }
      else {
        Assertions.assertThat(sent).isEmpty();
      }

      rule.allocator.assertHasNoLeaks();
    }
  }

  @ParameterizedTest
  @MethodSource("interactions")
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void shouldHaveNoLeaksOnPayloadInCaseOfRacingOfRequestAndCancel(
          BiFunction<RemotingClient, Publisher<Payload>, Publisher<?>> request, FrameType requestType) {
    Assumptions.assumeThat(requestType).isNotEqualTo(FrameType.REQUEST_CHANNEL);

    for (int i = 0; i < RaceTestConstants.REPEATS; i++) {
      ClientChannelRule rule = new ClientChannelRule();
      rule.init();
      ByteBuf dataBuffer = rule.allocator.buffer();
      dataBuffer.writeCharSequence("test", CharsetUtil.UTF_8);

      ByteBuf metadataBuffer = rule.allocator.buffer();
      metadataBuffer.writeCharSequence("testMetadata", CharsetUtil.UTF_8);

      Payload payload = ByteBufPayload.create(dataBuffer, metadataBuffer);
      AssertSubscriber assertSubscriber = AssertSubscriber.create(0);

      Publisher<?> publisher = request.apply(rule.client, Mono.just(payload));
      publisher.subscribe(assertSubscriber);

      RaceTestUtils.race(
              () -> {
                assertSubscriber.request(1);
                rule.delayer.run();
              },
              assertSubscriber::cancel);

      Collection<ByteBuf> sent = rule.connection.getSent();
      if (sent.size() == 1) {
        Assertions.assertThat(sent)
                .allMatch(bb -> FrameHeaderCodec.frameType(bb).equals(requestType))
                .allMatch(ReferenceCounted::release);
      }
      else if (sent.size() == 2) {
        Assertions.assertThat(sent)
                .first()
                .matches(bb -> FrameHeaderCodec.frameType(bb).equals(requestType))
                .matches(ReferenceCounted::release);
        Assertions.assertThat(sent)
                .element(1)
                .matches(bb -> FrameHeaderCodec.frameType(bb).equals(FrameType.CANCEL))
                .matches(ReferenceCounted::release);
      }
      else {
        Assertions.assertThat(sent).isEmpty();
      }

      rule.allocator.assertHasNoLeaks();
    }
  }

  @ParameterizedTest
  @MethodSource("interactions")
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void shouldPropagateDownstreamContext(
          BiFunction<RemotingClient, Publisher<Payload>, Publisher<?>> request, FrameType requestType) {
    Assumptions.assumeThat(requestType).isNotEqualTo(FrameType.REQUEST_CHANNEL);

    ByteBuf dataBuffer = rule.allocator.buffer();
    dataBuffer.writeCharSequence("test", CharsetUtil.UTF_8);

    ByteBuf metadataBuffer = rule.allocator.buffer();
    metadataBuffer.writeCharSequence("testMetadata", CharsetUtil.UTF_8);

    Payload payload = ByteBufPayload.create(dataBuffer, metadataBuffer);
    AssertSubscriber assertSubscriber = new AssertSubscriber(Context.of("test", "test"));

    ContextView[] receivedContext = new Context[1];
    Publisher<?> publisher =
            request.apply(
                    rule.client,
                    Mono.just(payload)
                            .mergeWith(
                                    Mono.deferContextual(
                                                    c -> {
                                                      receivedContext[0] = c;
                                                      return Mono.empty();
                                                    })
                                            .then(Mono.empty())));
    publisher.subscribe(assertSubscriber);

    rule.delayer.run();

    Collection<ByteBuf> sent = rule.connection.getSent();
    if (sent.size() == 1) {
      Assertions.assertThat(sent)
              .allMatch(bb -> FrameHeaderCodec.frameType(bb).equals(requestType))
              .allMatch(ReferenceCounted::release);
    }
    else if (sent.size() == 2) {
      Assertions.assertThat(sent)
              .first()
              .matches(bb -> FrameHeaderCodec.frameType(bb).equals(requestType))
              .matches(ReferenceCounted::release);
      Assertions.assertThat(sent)
              .element(1)
              .matches(bb -> FrameHeaderCodec.frameType(bb).equals(FrameType.CANCEL))
              .matches(ReferenceCounted::release);
    }
    else {
      Assertions.assertThat(sent).isEmpty();
    }

    Assertions.assertThat(receivedContext)
            .hasSize(1)
            .allSatisfy(
                    c ->
                            Assertions.assertThat(
                                            c.stream()
                                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                                    .containsKeys("test", DefaultRemotingClient.ON_DISCARD_KEY));

    rule.allocator.assertHasNoLeaks();
  }

  @ParameterizedTest
  @MethodSource("interactions")
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void shouldSupportMultiSubscriptionOnTheSameInteractionPublisher(
          BiFunction<RemotingClient, Publisher<Payload>, Publisher<?>> request, FrameType requestType) {
    AtomicBoolean once1 = new AtomicBoolean();
    AtomicBoolean once2 = new AtomicBoolean();
    Mono<Payload> source =
            Mono.fromCallable(
                            () -> {
                              if (!once1.getAndSet(true)) {
                                throw new IllegalStateException("test");
                              }
                              return ByteBufPayload.create("test", "testMetadata");
                            })
                    .doFinally(
                            st -> {
                              rule.delayer.run();
                              if (requestType != FrameType.METADATA_PUSH
                                      && requestType != FrameType.REQUEST_FNF) {
                                if (st != SignalType.ON_ERROR) {
                                  if (!once2.getAndSet(true)) {
                                    rule.connection.addToReceivedBuffer(
                                            ErrorFrameCodec.encode(
                                                    rule.allocator, 1, new IllegalStateException("test")));
                                  }
                                  else {
                                    rule.connection.addToReceivedBuffer(
                                            PayloadFrameCodec.encodeComplete(rule.allocator, 3));
                                  }
                                }
                              }
                            });
    AssertSubscriber assertSubscriber = AssertSubscriber.create(0);

    Publisher<?> publisher = request.apply(rule.client, source);
    if (publisher instanceof Mono) {
      ((Mono) publisher)
              .retryWhen(Retry.backoff(3, Duration.ofMillis(100)))
              .subscribe(assertSubscriber);
    }
    else {
      ((Flux) publisher)
              .retryWhen(Retry.backoff(3, Duration.ofMillis(100)))
              .subscribe(assertSubscriber);
    }

    assertSubscriber.request(1);

    if (requestType == FrameType.REQUEST_CHANNEL) {
      rule.delayer.run();
    }

    assertSubscriber.await(Duration.ofSeconds(10)).assertComplete();

    if (requestType == FrameType.REQUEST_CHANNEL) {
      ArrayList<ByteBuf> sent = new ArrayList<>(rule.connection.getSent());
      Assertions.assertThat(sent).hasSize(4);
      for (int i = 0; i < sent.size(); i++) {
        if (i % 2 == 0) {
          Assertions.assertThat(sent.get(i))
                  .matches(bb -> FrameHeaderCodec.frameType(bb).equals(requestType))
                  .matches(ReferenceCounted::release);
        }
        else {
          Assertions.assertThat(sent.get(i))
                  .matches(bb -> FrameHeaderCodec.frameType(bb).equals(FrameType.COMPLETE))
                  .matches(ReferenceCounted::release);
        }
      }
    }
    else {
      Collection<ByteBuf> sent = rule.connection.getSent();
      Assertions.assertThat(sent)
              .hasSize(
                      requestType == FrameType.REQUEST_FNF || requestType == FrameType.METADATA_PUSH
                              ? 1
                              : 2)
              .allMatch(bb -> FrameHeaderCodec.frameType(bb).equals(requestType))
              .allMatch(ReferenceCounted::release);
    }

    rule.allocator.assertHasNoLeaks();
  }

  @Test
  public void shouldBeAbleToResolveOriginalSource() {
    AssertSubscriber<Channel> assertSubscriber = AssertSubscriber.create(0);
    rule.client.source().subscribe(assertSubscriber);

    assertSubscriber.assertNotTerminated();

    rule.delayer.run();

    assertSubscriber.request(1);

    assertSubscriber.assertTerminated().assertValueCount(1);

    AssertSubscriber<Channel> assertSubscriber1 = AssertSubscriber.create();

    rule.client.source().subscribe(assertSubscriber1);

    assertSubscriber1.assertTerminated().assertValueCount(1);

    Assertions.assertThat(assertSubscriber1.values()).isEqualTo(assertSubscriber.values());

    rule.allocator.assertHasNoLeaks();
  }

  @Test
  public void shouldDisposeOriginalSource() {
    AssertSubscriber<Channel> assertSubscriber = AssertSubscriber.create();
    rule.client.source().subscribe(assertSubscriber);
    rule.delayer.run();
    assertSubscriber.assertTerminated().assertValueCount(1);

    rule.client.dispose();

    Assertions.assertThat(rule.client.isDisposed()).isTrue();

    AssertSubscriber<Channel> assertSubscriber1 = AssertSubscriber.create();

    rule.client.source().subscribe(assertSubscriber1);

    assertSubscriber1
            .assertTerminated()
            .assertError(CancellationException.class)
            .assertErrorMessage("Disposed");

    Assertions.assertThat(rule.channel.isDisposed()).isTrue();

    FrameAssert.assertThat(rule.connection.awaitFrame())
            .hasStreamIdZero()
            .hasData("Disposed")
            .hasNoLeaks();

    rule.allocator.assertHasNoLeaks();
  }

  @Test
  public void shouldReceiveOnCloseNotificationOnDisposeOriginalSource() {
    Sinks.Empty<Void> onCloseDelayer = Sinks.empty();
    ClientChannelRule rule =
            new ClientChannelRule() {
              @Override
              protected Channel newChannel() {
                return new ChannelWrapper(super.newChannel()) {
                  @Override
                  public Mono<Void> onClose() {
                    return super.onClose().and(onCloseDelayer.asMono());
                  }
                };
              }
            };
    rule.init();
    AssertSubscriber<Channel> assertSubscriber = AssertSubscriber.create();
    rule.client.source().subscribe(assertSubscriber);
    rule.delayer.run();
    assertSubscriber.assertTerminated().assertValueCount(1);

    rule.client.dispose();

    Assertions.assertThat(rule.client.isDisposed()).isTrue();

    AssertSubscriber<Void> onCloseSubscriber = AssertSubscriber.create();

    rule.client.onClose().subscribe(onCloseSubscriber);
    onCloseSubscriber.assertNotTerminated();

    onCloseDelayer.tryEmitEmpty();

    onCloseSubscriber.assertTerminated().assertComplete();

    Assertions.assertThat(rule.channel.isDisposed()).isTrue();

    FrameAssert.assertThat(rule.connection.awaitFrame())
            .hasStreamIdZero()
            .hasData("Disposed")
            .hasNoLeaks();

    rule.allocator.assertHasNoLeaks();
  }

  @Test
  public void shouldResolveOnStartSource() {
    AssertSubscriber<Channel> assertSubscriber = AssertSubscriber.create();
    Assertions.assertThat(rule.client.connect()).isTrue();
    rule.client.source().subscribe(assertSubscriber);
    rule.delayer.run();
    assertSubscriber.assertTerminated().assertValueCount(1);

    rule.client.dispose();

    Assertions.assertThat(rule.client.isDisposed()).isTrue();

    AssertSubscriber<Void> assertSubscriber1 = AssertSubscriber.create();

    rule.client.onClose().subscribe(assertSubscriber1);

    assertSubscriber1.assertTerminated().assertComplete();

    Assertions.assertThat(rule.channel.isDisposed()).isTrue();

    FrameAssert.assertThat(rule.connection.awaitFrame())
            .hasStreamIdZero()
            .hasData("Disposed")
            .hasNoLeaks();

    rule.allocator.assertHasNoLeaks();
  }

  @Test
  public void shouldNotStartIfAlreadyDisposed() {
    Assertions.assertThat(rule.client.connect()).isTrue();
    Assertions.assertThat(rule.client.connect()).isTrue();
    rule.delayer.run();

    rule.client.dispose();

    Assertions.assertThat(rule.client.connect()).isFalse();

    Assertions.assertThat(rule.client.isDisposed()).isTrue();

    AssertSubscriber<Void> assertSubscriber1 = AssertSubscriber.create();

    rule.client.onClose().subscribe(assertSubscriber1);

    assertSubscriber1.assertTerminated().assertComplete();

    Assertions.assertThat(rule.channel.isDisposed()).isTrue();

    FrameAssert.assertThat(rule.connection.awaitFrame())
            .hasStreamIdZero()
            .hasData("Disposed")
            .hasNoLeaks();

    rule.allocator.assertHasNoLeaks();
  }

  @Test
  public void shouldBeRestartedIfSourceWasClosed() {
    AssertSubscriber<Channel> assertSubscriber = AssertSubscriber.create();
    AssertSubscriber<Void> terminateSubscriber = AssertSubscriber.create();

    Assertions.assertThat(rule.client.connect()).isTrue();
    rule.client.source().subscribe(assertSubscriber);
    rule.client.onClose().subscribe(terminateSubscriber);

    rule.delayer.run();

    assertSubscriber.assertTerminated().assertValueCount(1);

    rule.channel.dispose();

    FrameAssert.assertThat(rule.connection.awaitFrame())
            .hasStreamIdZero()
            .hasData("Disposed")
            .hasNoLeaks();

    terminateSubscriber.assertNotTerminated();
    Assertions.assertThat(rule.client.isDisposed()).isFalse();

    rule.connection = new TestDuplexConnection(rule.allocator);
    rule.channel = rule.newChannel();
    rule.producer = Sinks.one();

    AssertSubscriber<Channel> assertSubscriber2 = AssertSubscriber.create();

    Assertions.assertThat(rule.client.connect()).isTrue();
    rule.client.source().subscribe(assertSubscriber2);

    rule.delayer.run();

    assertSubscriber2.assertTerminated().assertValueCount(1);

    rule.client.dispose();

    terminateSubscriber.assertTerminated().assertComplete();

    Assertions.assertThat(rule.client.connect()).isFalse();

    Assertions.assertThat(rule.channel.isDisposed()).isTrue();

    FrameAssert.assertThat(rule.connection.awaitFrame())
            .hasStreamIdZero()
            .hasData("Disposed")
            .hasNoLeaks();

    rule.allocator.assertHasNoLeaks();
  }

  @Test
  public void shouldDisposeOriginalSourceIfRacing() {
    for (int i = 0; i < RaceTestConstants.REPEATS; i++) {
      ClientChannelRule rule = new ClientChannelRule();

      rule.init();

      AssertSubscriber<Channel> assertSubscriber = AssertSubscriber.create();
      rule.client.source().subscribe(assertSubscriber);

      RaceTestUtils.race(rule.delayer, () -> rule.client.dispose());

      assertSubscriber.assertTerminated();

      Assertions.assertThat(rule.client.isDisposed()).isTrue();
      Assertions.assertThat(rule.channel.isDisposed()).isTrue();

      AssertSubscriber<Channel> assertSubscriber1 = AssertSubscriber.create();

      rule.client.source().subscribe(assertSubscriber1);

      assertSubscriber1
              .assertTerminated()
              .assertError(CancellationException.class)
              .assertErrorMessage("Disposed");

      ByteBuf buf;
      while ((buf = rule.connection.pollFrame()) != null) {
        FrameAssert.assertThat(buf).hasStreamIdZero().hasData("Disposed").hasNoLeaks();
      }

      rule.allocator.assertHasNoLeaks();
    }
  }

  @Test
  public void shouldStartOriginalSourceOnceIfRacing() {
    for (int i = 0; i < RaceTestConstants.REPEATS; i++) {
      ClientChannelRule rule = new ClientChannelRule();

      rule.init();

      AssertSubscriber<Channel> assertSubscriber = AssertSubscriber.create();

      RaceTestUtils.race(
              () -> rule.client.source().subscribe(assertSubscriber), () -> rule.client.connect());

      Assertions.assertThat(rule.producer.currentSubscriberCount()).isOne();

      rule.delayer.run();

      assertSubscriber.assertTerminated();

      rule.client.dispose();

      Assertions.assertThat(rule.client.isDisposed()).isTrue();
      Assertions.assertThat(rule.channel.isDisposed()).isTrue();

      AssertSubscriber<Void> assertSubscriber1 = AssertSubscriber.create();

      rule.client.onClose().subscribe(assertSubscriber1);
      FrameAssert.assertThat(rule.connection.awaitFrame())
              .hasStreamIdZero()
              .hasData("Disposed")
              .hasNoLeaks();

      assertSubscriber1.assertTerminated().assertComplete();

      rule.allocator.assertHasNoLeaks();
    }
  }

  public static class ClientChannelRule extends AbstractChannelRule<Channel> {

    protected RemotingClient client;
    protected Runnable delayer;
    protected Sinks.One<Channel> producer;

    protected Sinks.Empty<Void> thisClosedSink;

    @Override
    protected void doInit() {
      super.doInit();
      delayer = () -> producer.tryEmitValue(channel);
      producer = Sinks.one();
      client =
              new DefaultRemotingClient(
                      Mono.defer(
                              () ->
                                      producer
                                              .asMono()
                                              .doOnCancel(() -> channel.dispose())
                                              .doOnDiscard(Disposable.class, Disposable::dispose)));
    }

    @Override
    protected Channel newChannel() {
      this.thisClosedSink = Sinks.empty();
      return new RequesterChannel(
              connection,
              PayloadDecoder.ZERO_COPY,
              StreamIdProvider.forClient(),
              0,
              maxFrameLength,
              maxInboundPayloadSize,
              Integer.MAX_VALUE,
              Integer.MAX_VALUE,
              null,
              __ -> null,
              null,
              thisClosedSink,
              thisClosedSink.asMono());
    }
  }
}
