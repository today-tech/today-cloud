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

import org.assertj.core.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import io.netty.buffer.ByteBuf;
import infra.remoting.FrameAssert;
import infra.remoting.Payload;
import infra.remoting.PayloadAssert;
import infra.remoting.Channel;
import infra.remoting.buffer.LeaksTrackingByteBufAllocator;
import infra.remoting.frame.FrameType;
import infra.remoting.internal.subscriber.AssertSubscriber;
import infra.remoting.plugins.RequestInterceptor;
import infra.remoting.plugins.TestRequestInterceptor;
import infra.remoting.test.util.TestDuplexConnection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.test.publisher.TestPublisher;

import static infra.remoting.frame.FrameType.METADATA_PUSH;
import static infra.remoting.frame.FrameType.NEXT;
import static infra.remoting.frame.FrameType.REQUEST_CHANNEL;
import static infra.remoting.frame.FrameType.REQUEST_FNF;
import static infra.remoting.frame.FrameType.REQUEST_RESPONSE;
import static infra.remoting.frame.FrameType.REQUEST_STREAM;

public class ResponderOperatorsCommonTests {

  interface Scenario {
    FrameType requestType();

    int maxElements();

    ResponderFrameHandler responseOperator(
            long initialRequestN,
            Payload firstPayload,
            TestChannelSupport streamManager,
            Channel handler);

    ResponderFrameHandler responseOperator(
            long initialRequestN,
            ByteBuf firstFragment,
            TestChannelSupport streamManager,
            Channel handler);
  }

  static Stream<Scenario> scenarios() {
    return Stream.of(
            new Scenario() {
              @Override
              public FrameType requestType() {
                return FrameType.REQUEST_RESPONSE;
              }

              @Override
              public int maxElements() {
                return 1;
              }

              @Override
              public ResponderFrameHandler responseOperator(
                      long initialRequestN,
                      ByteBuf firstFragment,
                      TestChannelSupport streamManager,
                      Channel handler) {
                int streamId = streamManager.getNextStreamId();
                RequestResponseResponderSubscriber subscriber =
                        new RequestResponseResponderSubscriber(
                                streamId, firstFragment, streamManager, handler);
                streamManager.activeStreams.put(streamId, subscriber);

                final RequestInterceptor requestInterceptor = streamManager.getRequestInterceptor();
                if (requestInterceptor != null) {
                  requestInterceptor.onStart(streamId, REQUEST_RESPONSE, null);
                }

                return subscriber;
              }

              @Override
              public ResponderFrameHandler responseOperator(
                      long initialRequestN,
                      Payload firstPayload,
                      TestChannelSupport streamManager,
                      Channel handler) {
                int streamId = streamManager.getNextStreamId();
                RequestResponseResponderSubscriber subscriber =
                        new RequestResponseResponderSubscriber(streamId, streamManager);
                streamManager.activeStreams.put(streamId, subscriber);

                final RequestInterceptor requestInterceptor = streamManager.getRequestInterceptor();
                if (requestInterceptor != null) {
                  requestInterceptor.onStart(streamId, REQUEST_RESPONSE, null);
                }

                return handler.requestResponse(firstPayload).subscribeWith(subscriber);
              }

              @Override
              public String toString() {
                return RequestResponseRequesterMono.class.getSimpleName();
              }
            },
            new Scenario() {
              @Override
              public FrameType requestType() {
                return FrameType.REQUEST_STREAM;
              }

              @Override
              public int maxElements() {
                return Integer.MAX_VALUE;
              }

              @Override
              public ResponderFrameHandler responseOperator(
                      long initialRequestN,
                      ByteBuf firstFragment,
                      TestChannelSupport streamManager,
                      Channel handler) {
                int streamId = streamManager.getNextStreamId();
                RequestStreamResponderSubscriber subscriber =
                        new RequestStreamResponderSubscriber(
                                streamId, initialRequestN, firstFragment, streamManager, handler);

                final RequestInterceptor requestInterceptor = streamManager.getRequestInterceptor();
                if (requestInterceptor != null) {
                  requestInterceptor.onStart(streamId, REQUEST_STREAM, null);
                }

                streamManager.activeStreams.put(streamId, subscriber);
                return subscriber;
              }

              @Override
              public ResponderFrameHandler responseOperator(
                      long initialRequestN,
                      Payload firstPayload,
                      TestChannelSupport streamManager,
                      Channel handler) {
                int streamId = streamManager.getNextStreamId();
                RequestStreamResponderSubscriber subscriber =
                        new RequestStreamResponderSubscriber(streamId, initialRequestN, streamManager);
                streamManager.activeStreams.put(streamId, subscriber);

                final RequestInterceptor requestInterceptor = streamManager.getRequestInterceptor();
                if (requestInterceptor != null) {
                  requestInterceptor.onStart(streamId, REQUEST_STREAM, null);
                }

                return handler.requestStream(firstPayload).subscribeWith(subscriber);
              }

              @Override
              public String toString() {
                return RequestStreamResponderSubscriber.class.getSimpleName();
              }
            },
            new Scenario() {
              @Override
              public FrameType requestType() {
                return FrameType.REQUEST_CHANNEL;
              }

              @Override
              public int maxElements() {
                return Integer.MAX_VALUE;
              }

              @Override
              public ResponderFrameHandler responseOperator(
                      long initialRequestN,
                      ByteBuf firstFragment,
                      TestChannelSupport streamManager,
                      Channel handler) {
                int streamId = streamManager.getNextStreamId();
                RequestChannelResponderSubscriber subscriber =
                        new RequestChannelResponderSubscriber(
                                streamId, initialRequestN, firstFragment, streamManager, handler);
                streamManager.activeStreams.put(streamId, subscriber);

                final RequestInterceptor requestInterceptor = streamManager.getRequestInterceptor();
                if (requestInterceptor != null) {
                  requestInterceptor.onStart(streamId, REQUEST_CHANNEL, null);
                }

                return subscriber;
              }

              @Override
              public ResponderFrameHandler responseOperator(
                      long initialRequestN,
                      Payload firstPayload,
                      TestChannelSupport streamManager,
                      Channel handler) {
                int streamId = streamManager.getNextStreamId();
                RequestChannelResponderSubscriber responderSubscriber =
                        new RequestChannelResponderSubscriber(
                                streamId, initialRequestN, firstPayload, streamManager);
                streamManager.activeStreams.put(streamId, responderSubscriber);

                final RequestInterceptor requestInterceptor = streamManager.getRequestInterceptor();
                if (requestInterceptor != null) {
                  requestInterceptor.onStart(streamId, REQUEST_CHANNEL, null);
                }

                return handler.requestChannel(responderSubscriber).subscribeWith(responderSubscriber);
              }

              @Override
              public String toString() {
                return RequestChannelResponderSubscriber.class.getSimpleName();
              }
            });
  }

  static class TestHandler implements Channel {

    final TestPublisher<Payload> producer;
    final AssertSubscriber<Payload> consumer;

    TestHandler(TestPublisher<Payload> producer, AssertSubscriber<Payload> consumer) {
      this.producer = producer;
      this.consumer = consumer;
    }

    @Override
    public Mono<Void> fireAndForget(Payload payload) {
      consumer.onSubscribe(Operators.emptySubscription());
      consumer.onNext(payload);
      consumer.onComplete();
      return producer.mono().then();
    }

    @Override
    public Mono<Payload> requestResponse(Payload payload) {
      consumer.onSubscribe(Operators.emptySubscription());
      consumer.onNext(payload);
      consumer.onComplete();
      return producer.mono();
    }

    @Override
    public Flux<Payload> requestStream(Payload payload) {
      consumer.onSubscribe(Operators.emptySubscription());
      consumer.onNext(payload);
      consumer.onComplete();
      return producer.flux();
    }

    @Override
    public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
      payloads.subscribe(consumer);
      return producer.flux();
    }
  }

  @ParameterizedTest
  @MethodSource("scenarios")
  void shouldHandleRequest(Scenario scenario) {
    Assumptions.assumeThat(scenario.requestType()).isNotIn(REQUEST_FNF, METADATA_PUSH);

    TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();
    TestChannelSupport testRequesterResponderSupport =
            TestChannelSupport.client(testRequestInterceptor);
    final LeaksTrackingByteBufAllocator allocator = testRequesterResponderSupport.getAllocator();
    final TestDuplexConnection sender = testRequesterResponderSupport.getDuplexConnection();
    TestPublisher<Payload> testPublisher = TestPublisher.create();
    TestHandler testHandler = new TestHandler(testPublisher, new AssertSubscriber<>(0));

    ResponderFrameHandler responderFrameHandler =
            scenario.responseOperator(
                    Long.MAX_VALUE,
                    TestChannelSupport.genericPayload(allocator),
                    testRequesterResponderSupport,
                    testHandler);

    Payload randomPayload = TestChannelSupport.randomPayload(allocator);
    testPublisher.assertWasSubscribed();
    testPublisher.next(randomPayload.retain());
    testPublisher.complete();

    FrameAssert.assertThat(sender.awaitFrame())
            .isNotNull()
            .hasStreamId(1)
            .typeOf(scenario.requestType() == REQUEST_RESPONSE ? FrameType.NEXT_COMPLETE : NEXT)
            .hasPayloadSize(
                    randomPayload.data().readableBytes() + randomPayload.sliceMetadata().readableBytes())
            .hasData(randomPayload.data())
            .hasNoLeaks();

    PayloadAssert.assertThat(randomPayload).hasNoLeaks();

    if (scenario.requestType() != REQUEST_RESPONSE) {

      FrameAssert.assertThat(sender.awaitFrame())
              .typeOf(FrameType.COMPLETE)
              .hasStreamId(1)
              .hasNoLeaks();

      if (scenario.requestType() == REQUEST_CHANNEL) {
        testHandler.consumer.request(2);
        FrameAssert.assertThat(sender.awaitFrame())
                .typeOf(FrameType.REQUEST_N)
                .hasStreamId(1)
                .hasRequestN(1)
                .hasNoLeaks();

        responderFrameHandler.handleComplete();
        testHandler.consumer.assertComplete();
      }
    }

    testHandler
            .consumer
            .assertValueCount(1)
            .assertValuesWith(p -> PayloadAssert.assertThat(p).hasNoLeaks());

    testRequestInterceptor
            .expectOnStart(1, scenario.requestType())
            .expectOnComplete(1)
            .expectNothing();
    allocator.assertHasNoLeaks();
  }

  @ParameterizedTest
  @MethodSource("scenarios")
  void shouldHandleFragmentedRequest(Scenario scenario) {
    Assumptions.assumeThat(scenario.requestType()).isNotIn(REQUEST_FNF, METADATA_PUSH);

    TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();
    TestChannelSupport testRequesterResponderSupport =
            TestChannelSupport.client(testRequestInterceptor);
    final LeaksTrackingByteBufAllocator allocator = testRequesterResponderSupport.getAllocator();
    final TestDuplexConnection sender = testRequesterResponderSupport.getDuplexConnection();
    TestPublisher<Payload> testPublisher = TestPublisher.create();
    TestHandler testHandler = new TestHandler(testPublisher, new AssertSubscriber<>(0));

    int mtu = ThreadLocalRandom.current().nextInt(64, 256);
    Payload firstPayload = TestChannelSupport.randomPayload(allocator);
    ArrayList<ByteBuf> fragments =
            TestChannelSupport.prepareFragments(allocator, mtu, firstPayload);

    ByteBuf firstFragment = fragments.remove(0);
    ResponderFrameHandler responderFrameHandler =
            scenario.responseOperator(
                    Long.MAX_VALUE, firstFragment, testRequesterResponderSupport, testHandler);
    firstFragment.release();

    testPublisher.assertWasNotSubscribed();
    testRequesterResponderSupport.assertHasStream(1, responderFrameHandler);

    for (int i = 0; i < fragments.size(); i++) {
      ByteBuf fragment = fragments.get(i);
      boolean hasFollows = i != fragments.size() - 1;
      responderFrameHandler.handleNext(fragment, hasFollows, !hasFollows);
      fragment.release();
    }

    Payload randomPayload = TestChannelSupport.randomPayload(allocator);
    testPublisher.assertWasSubscribed();
    testPublisher.next(randomPayload.retain());
    testPublisher.complete();

    FrameAssert.assertThat(sender.awaitFrame())
            .isNotNull()
            .hasStreamId(1)
            .typeOf(scenario.requestType() == REQUEST_RESPONSE ? FrameType.NEXT_COMPLETE : NEXT)
            .hasPayloadSize(
                    randomPayload.data().readableBytes() + randomPayload.sliceMetadata().readableBytes())
            .hasData(randomPayload.data())
            .hasNoLeaks();

    PayloadAssert.assertThat(randomPayload).hasNoLeaks();

    if (scenario.requestType() != REQUEST_RESPONSE) {

      FrameAssert.assertThat(sender.awaitFrame())
              .typeOf(FrameType.COMPLETE)
              .hasStreamId(1)
              .hasNoLeaks();

      if (scenario.requestType() == REQUEST_CHANNEL) {
        testHandler.consumer.request(2);
        FrameAssert.assertThat(sender.pollFrame()).isNull();
      }
    }

    testHandler
            .consumer
            .assertValueCount(1)
            .assertValuesWith(
                    p -> PayloadAssert.assertThat(p).hasData(firstPayload.sliceData()).hasNoLeaks())
            .assertComplete();

    testRequesterResponderSupport.assertNoActiveStreams();

    firstPayload.release();

    testRequestInterceptor
            .expectOnStart(1, scenario.requestType())
            .expectOnComplete(1)
            .expectNothing();

    allocator.assertHasNoLeaks();
  }

  @ParameterizedTest
  @MethodSource("scenarios")
  void shouldHandleInterruptedFragmentation(Scenario scenario) {
    Assumptions.assumeThat(scenario.requestType()).isNotIn(REQUEST_FNF, METADATA_PUSH);

    final TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();
    TestChannelSupport testRequesterResponderSupport =
            TestChannelSupport.client(testRequestInterceptor);
    final LeaksTrackingByteBufAllocator allocator = testRequesterResponderSupport.getAllocator();
    TestPublisher<Payload> testPublisher = TestPublisher.create();
    TestHandler testHandler = new TestHandler(testPublisher, new AssertSubscriber<>(0));

    int mtu = ThreadLocalRandom.current().nextInt(64, 256);
    Payload firstPayload = TestChannelSupport.randomPayload(allocator);
    ArrayList<ByteBuf> fragments =
            TestChannelSupport.prepareFragments(allocator, mtu, firstPayload);
    firstPayload.release();

    ByteBuf firstFragment = fragments.remove(0);
    ResponderFrameHandler responderFrameHandler =
            scenario.responseOperator(
                    Long.MAX_VALUE, firstFragment, testRequesterResponderSupport, testHandler);
    firstFragment.release();

    testPublisher.assertWasNotSubscribed();
    testRequesterResponderSupport.assertHasStream(1, responderFrameHandler);

    for (int i = 0; i < fragments.size(); i++) {
      ByteBuf fragment = fragments.get(i);
      boolean hasFollows = i != fragments.size() - 1;
      if (hasFollows) {
        responderFrameHandler.handleNext(fragment, true, false);
      }
      else {
        responderFrameHandler.handleCancel();
      }
      fragment.release();
    }

    testPublisher.assertWasNotSubscribed();
    testRequesterResponderSupport.assertNoActiveStreams();

    testRequestInterceptor
            .expectOnStart(1, scenario.requestType())
            .expectOnCancel(1)
            .expectNothing();

    allocator.assertHasNoLeaks();
  }
}
