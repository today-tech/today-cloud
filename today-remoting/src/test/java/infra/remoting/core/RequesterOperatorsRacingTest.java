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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.CharsetUtil;
import infra.remoting.FrameAssert;
import infra.remoting.Payload;
import infra.remoting.RaceTestConstants;
import infra.remoting.frame.FrameType;
import infra.remoting.frame.RequestStreamFrameCodec;
import infra.remoting.internal.subscriber.AssertSubscriber;
import infra.remoting.plugins.TestRequestInterceptor;
import infra.remoting.util.ByteBufPayload;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.test.StepVerifier;
import reactor.test.util.RaceTestUtils;

import static infra.remoting.frame.FrameType.COMPLETE;
import static infra.remoting.frame.FrameType.METADATA_PUSH;
import static infra.remoting.frame.FrameType.REQUEST_CHANNEL;
import static infra.remoting.frame.FrameType.REQUEST_N;
import static infra.remoting.frame.FrameType.REQUEST_RESPONSE;
import static infra.remoting.frame.FrameType.REQUEST_STREAM;

@SuppressWarnings("ALL")
public class RequesterOperatorsRacingTest {

  interface Scenario {
    FrameType requestType();

    Publisher<?> requestOperator(
            Supplier<Payload> payloadsSupplier, ChannelSupport channelSupport);
  }

  static Stream<Scenario> scenarios() {
    return Stream.of(
            new Scenario() {
              @Override
              public FrameType requestType() {
                return METADATA_PUSH;
              }

              @Override
              public Publisher<?> requestOperator(
                      Supplier<Payload> payloadsSupplier,
                      ChannelSupport channelSupport) {
                return new MetadataPushRequesterMono(payloadsSupplier.get(), channelSupport);
              }

              @Override
              public String toString() {
                return MetadataPushRequesterMono.class.getSimpleName();
              }
            },
            new Scenario() {
              @Override
              public FrameType requestType() {
                return FrameType.REQUEST_FNF;
              }

              @Override
              public Publisher<?> requestOperator(
                      Supplier<Payload> payloadsSupplier,
                      ChannelSupport channelSupport) {
                return new FireAndForgetRequesterMono(
                        payloadsSupplier.get(), channelSupport);
              }

              @Override
              public String toString() {
                return FireAndForgetRequesterMono.class.getSimpleName();
              }
            },
            new Scenario() {
              @Override
              public FrameType requestType() {
                return FrameType.REQUEST_RESPONSE;
              }

              @Override
              public Publisher<?> requestOperator(
                      Supplier<Payload> payloadsSupplier,
                      ChannelSupport channelSupport) {
                return new RequestResponseRequesterMono(
                        payloadsSupplier.get(), channelSupport);
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
              public Publisher<?> requestOperator(
                      Supplier<Payload> payloadsSupplier,
                      ChannelSupport channelSupport) {
                return new RequestStreamRequesterFlux(
                        payloadsSupplier.get(), channelSupport);
              }

              @Override
              public String toString() {
                return RequestStreamRequesterFlux.class.getSimpleName();
              }
            },
            new Scenario() {
              @Override
              public FrameType requestType() {
                return FrameType.REQUEST_CHANNEL;
              }

              @Override
              public Publisher<?> requestOperator(
                      Supplier<Payload> payloadsSupplier,
                      ChannelSupport channelSupport) {
                return new RequestChannelRequesterFlux(
                        Flux.generate(s -> s.next(payloadsSupplier.get())), channelSupport);
              }

              @Override
              public String toString() {
                return RequestChannelRequesterFlux.class.getSimpleName();
              }
            });
  }

  /*
   * +--------------------------------+
   * |       Racing Test Cases        |
   * +--------------------------------+
   */

  /** Ensures single subscription happens in case of racing */
  @ParameterizedTest(name = "Should subscribe exactly once to {0}")
  @MethodSource("scenarios")
  public void shouldSubscribeExactlyOnce(Scenario scenario) {
    for (int i = 0; i < RaceTestConstants.REPEATS; i++) {
      final TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();
      final TestChannelSupport requesterResponderSupport =
              TestChannelSupport.client(testRequestInterceptor);
      final Supplier<Payload> payloadSupplier =
              () ->
                      TestChannelSupport.genericPayload(
                              requesterResponderSupport.getAllocator());

      final Publisher<?> requestOperator =
              scenario.requestOperator(payloadSupplier, requesterResponderSupport);

      StepVerifier stepVerifier =
              StepVerifier.create(requesterResponderSupport.getDuplexConnection().getSentAsPublisher())
                      .assertNext(
                              frame -> {
                                FrameAssert frameAssert =
                                        FrameAssert.assertThat(frame)
                                                .isNotNull()
                                                .hasNoFragmentsFollow()
                                                .typeOf(scenario.requestType());
                                if (scenario.requestType() == METADATA_PUSH) {
                                  frameAssert
                                          .hasStreamIdZero()
                                          .hasPayloadSize(
                                                  TestChannelSupport.METADATA_CONTENT.getBytes(
                                                          CharsetUtil.UTF_8)
                                                          .length)
                                          .hasMetadata(TestChannelSupport.METADATA_CONTENT);
                                }
                                else {
                                  frameAssert
                                          .hasClientSideStreamId()
                                          .hasStreamId(1)
                                          .hasPayloadSize(
                                                  TestChannelSupport.METADATA_CONTENT.getBytes(
                                                          CharsetUtil.UTF_8)
                                                          .length
                                                          + TestChannelSupport.DATA_CONTENT.getBytes(
                                                          CharsetUtil.UTF_8)
                                                          .length)
                                          .hasMetadata(TestChannelSupport.METADATA_CONTENT)
                                          .hasData(TestChannelSupport.DATA_CONTENT);
                                }
                                frameAssert.hasNoLeaks();

                                if (requestOperator instanceof FrameHandler) {
                                  ((FrameHandler) requestOperator).handleComplete();
                                  if (scenario.requestType() == REQUEST_CHANNEL) {
                                    ((FrameHandler) requestOperator).handleCancel();
                                  }
                                }
                              })
                      .thenCancel()
                      .verifyLater();

      Assertions.assertThatThrownBy(
                      () ->
                              RaceTestUtils.race(
                                      () -> {
                                        AssertSubscriber subscriber = new AssertSubscriber<>();
                                        requestOperator.subscribe(subscriber);
                                        subscriber.await().assertTerminated().assertNoError();
                                      },
                                      () -> {
                                        AssertSubscriber subscriber = new AssertSubscriber<>();
                                        requestOperator.subscribe(subscriber);
                                        subscriber.await().assertTerminated().assertNoError();
                                      }))
              .matches(
                      t -> {
                        Assertions.assertThat(t).hasMessageContaining("allows only a single Subscriber");
                        return true;
                      });

      stepVerifier.verify(Duration.ofSeconds(1));
      requesterResponderSupport.getAllocator().assertHasNoLeaks();
      if (scenario.requestType() != METADATA_PUSH) {
        testRequestInterceptor
                .assertNext(
                        event ->
                                Assertions.assertThat(event.eventType)
                                        .isIn(
                                                TestRequestInterceptor.EventType.ON_START,
                                                TestRequestInterceptor.EventType.ON_REJECT))
                .assertNext(
                        event ->
                                Assertions.assertThat(event.eventType)
                                        .isIn(
                                                TestRequestInterceptor.EventType.ON_START,
                                                TestRequestInterceptor.EventType.ON_COMPLETE,
                                                TestRequestInterceptor.EventType.ON_REJECT))
                .assertNext(
                        event ->
                                Assertions.assertThat(event.eventType)
                                        .isIn(
                                                TestRequestInterceptor.EventType.ON_COMPLETE,
                                                TestRequestInterceptor.EventType.ON_REJECT))
                .expectNothing();
      }
    }
  }

  /** Ensures single frame is sent only once racing between requests */
  @ParameterizedTest(name = "{0} should sent requestFrame exactly once if request(n) is racing")
  @MethodSource("scenarios")
  public void shouldSentRequestFrameOnceInCaseOfRequestRacing(Scenario scenario) {
    Assumptions.assumeThat(scenario.requestType())
            .isIn(REQUEST_RESPONSE, REQUEST_STREAM, REQUEST_CHANNEL);

    for (int i = 0; i < RaceTestConstants.REPEATS; i++) {
      final TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();
      final TestChannelSupport activeStreams =
              TestChannelSupport.client(testRequestInterceptor);
      final Supplier<Payload> payloadSupplier =
              () -> TestChannelSupport.genericPayload(activeStreams.getAllocator());

      final Publisher<Payload> requestOperator =
              (Publisher<Payload>) scenario.requestOperator(payloadSupplier, activeStreams);

      Payload response = ByteBufPayload.create("test", "test");

      final AssertSubscriber<Payload> assertSubscriber = AssertSubscriber.create(0);
      requestOperator.subscribe(assertSubscriber);

      RaceTestUtils.race(() -> assertSubscriber.request(1), () -> assertSubscriber.request(1));

      final ByteBuf sentFrame = activeStreams.getDuplexConnection().awaitFrame();

      if (scenario.requestType().hasInitialRequestN()) {
        if (RequestStreamFrameCodec.initialRequestN(sentFrame) == 1) {
          FrameAssert.assertThat(activeStreams.getDuplexConnection().awaitFrame())
                  .isNotNull()
                  .hasStreamId(1)
                  .hasRequestN(1)
                  .typeOf(REQUEST_N)
                  .hasNoLeaks();
        }
        else {
          Assertions.assertThat(RequestStreamFrameCodec.initialRequestN(sentFrame)).isEqualTo(2);
        }
      }

      FrameAssert.assertThat(sentFrame)
              .isNotNull()
              .hasPayloadSize(
                      TestChannelSupport.DATA_CONTENT.getBytes(CharsetUtil.UTF_8).length
                              + TestChannelSupport.METADATA_CONTENT.getBytes(CharsetUtil.UTF_8)
                              .length)
              .hasMetadata(TestChannelSupport.METADATA_CONTENT)
              .hasData(TestChannelSupport.DATA_CONTENT)
              .hasNoFragmentsFollow()
              .typeOf(scenario.requestType())
              .hasClientSideStreamId()
              .hasStreamId(1)
              .hasNoLeaks();

      ((RequesterFrameHandler) requestOperator).handlePayload(response);
      ((RequesterFrameHandler) requestOperator).handleComplete();

      if (scenario.requestType() == REQUEST_CHANNEL) {
        ((CoreSubscriber) requestOperator).onComplete();
        FrameAssert.assertThat(activeStreams.getDuplexConnection().awaitFrame())
                .typeOf(COMPLETE)
                .hasStreamId(1)
                .hasNoLeaks();
      }

      assertSubscriber
              .assertTerminated()
              .assertValuesWith(
                      p -> {
                        Assertions.assertThat(p.release()).isTrue();
                        Assertions.assertThat(p.refCnt()).isZero();
                      });

      activeStreams.assertNoActiveStreams();
      Assertions.assertThat(activeStreams.getDuplexConnection().isEmpty()).isTrue();
      activeStreams.getAllocator().assertHasNoLeaks();
      if (scenario.requestType() != METADATA_PUSH) {
        testRequestInterceptor
                .expectOnStart(1, scenario.requestType())
                .expectOnComplete(1)
                .expectNothing();
      }
    }
  }

  /**
   * Ensures that no ByteBuf is leaked if reassembly is starting and cancel is happening at the same
   * time
   */
  @ParameterizedTest(name = "Should have no leaks when {0} is canceled during reassembly")
  @MethodSource("scenarios")
  public void shouldHaveNoLeaksOnReassemblyAndCancelRacing(Scenario scenario) {
    Assumptions.assumeThat(scenario.requestType())
            .isIn(REQUEST_RESPONSE, REQUEST_STREAM, REQUEST_CHANNEL);

    for (int i = 0; i < RaceTestConstants.REPEATS; i++) {
      final TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();
      final TestChannelSupport activeStreams =
              TestChannelSupport.client(testRequestInterceptor);
      final Supplier<Payload> payloadSupplier =
              () -> TestChannelSupport.genericPayload(activeStreams.getAllocator());

      final Publisher<Payload> requestOperator =
              (Publisher<Payload>) scenario.requestOperator(payloadSupplier, activeStreams);

      final AssertSubscriber<Payload> assertSubscriber = new AssertSubscriber<>(1);

      requestOperator.subscribe(assertSubscriber);

      final ByteBuf sentFrame = activeStreams.getDuplexConnection().awaitFrame();
      FrameAssert.assertThat(sentFrame)
              .isNotNull()
              .hasPayloadSize(
                      TestChannelSupport.DATA_CONTENT.getBytes(CharsetUtil.UTF_8).length
                              + TestChannelSupport.METADATA_CONTENT.getBytes(CharsetUtil.UTF_8)
                              .length)
              .hasMetadata(TestChannelSupport.METADATA_CONTENT)
              .hasData(TestChannelSupport.DATA_CONTENT)
              .hasNoFragmentsFollow()
              .typeOf(scenario.requestType())
              .hasClientSideStreamId()
              .hasStreamId(1)
              .hasNoLeaks();

      int mtu = ThreadLocalRandom.current().nextInt(64, 256);
      Payload responsePayload =
              TestChannelSupport.randomPayload(activeStreams.getAllocator());
      ArrayList<ByteBuf> fragments =
              TestChannelSupport.prepareFragments(
                      activeStreams.getAllocator(), mtu, responsePayload);
      RaceTestUtils.race(
              assertSubscriber::cancel,
              () -> {
                FrameHandler frameHandler = (FrameHandler) requestOperator;
                int lastFragmentId = fragments.size() - 1;
                for (int j = 0; j < fragments.size(); j++) {
                  ByteBuf frame = fragments.get(j);
                  frameHandler.handleNext(frame, lastFragmentId != j, lastFragmentId == j);
                  frame.release();
                }
              });

      List<Payload> values = assertSubscriber.values();
      if (!values.isEmpty()) {
        Assertions.assertThat(values)
                .hasSize(1)
                .first()
                .matches(
                        p -> {
                          Assertions.assertThat(p.sliceData())
                                  .matches(bb -> ByteBufUtil.equals(bb, responsePayload.sliceData()));
                          Assertions.assertThat(p.hasMetadata()).isEqualTo(responsePayload.hasMetadata());
                          Assertions.assertThat(p.sliceMetadata())
                                  .matches(bb -> ByteBufUtil.equals(bb, responsePayload.sliceMetadata()));
                          Assertions.assertThat(p.release()).isTrue();
                          Assertions.assertThat(p.refCnt()).isZero();
                          return true;
                        });
      }

      if (!activeStreams.getDuplexConnection().isEmpty()) {
        if (scenario.requestType() != REQUEST_CHANNEL) {
          assertSubscriber.assertNotTerminated();
        }

        final ByteBuf cancellationFrame = activeStreams.getDuplexConnection().awaitFrame();
        FrameAssert.assertThat(cancellationFrame)
                .isNotNull()
                .typeOf(FrameType.CANCEL)
                .hasClientSideStreamId()
                .hasStreamId(1)
                .hasNoLeaks();

        testRequestInterceptor
                .expectOnStart(1, scenario.requestType())
                .expectOnCancel(1)
                .expectNothing();
      }
      else {
        testRequestInterceptor
                .expectOnStart(1, scenario.requestType())
                .expectOnComplete(1)
                .expectNothing();
      }

      Assertions.assertThat(responsePayload.release()).isTrue();
      Assertions.assertThat(responsePayload.refCnt()).isZero();

      activeStreams.assertNoActiveStreams();
      Assertions.assertThat(activeStreams.getDuplexConnection().isEmpty()).isTrue();
      activeStreams.getAllocator().assertHasNoLeaks();
    }
  }

  /**
   * Ensures that in case of racing between next element and cancel we will not have any memory
   * leaks
   */
  @ParameterizedTest(name = "Should have no leaks when {0} is canceled during reassembly")
  @MethodSource("scenarios")
  public void shouldHaveNoLeaksOnNextAndCancelRacing(Scenario scenario) {
    Assumptions.assumeThat(scenario.requestType())
            .isIn(REQUEST_RESPONSE, REQUEST_STREAM, REQUEST_CHANNEL);

    for (int i = 0; i < RaceTestConstants.REPEATS; i++) {
      final TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();
      final TestChannelSupport activeStreams =
              TestChannelSupport.client(testRequestInterceptor);
      final Supplier<Payload> payloadSupplier =
              () -> TestChannelSupport.genericPayload(activeStreams.getAllocator());

      final Publisher<?> requestOperator = scenario.requestOperator(payloadSupplier, activeStreams);

      Payload response = ByteBufPayload.create("test", "test");
      AssertSubscriber<Payload> assertSubscriber = AssertSubscriber.create();
      requestOperator.subscribe((AssertSubscriber) assertSubscriber);

      final ByteBuf sentFrame = activeStreams.getDuplexConnection().awaitFrame();
      FrameAssert.assertThat(sentFrame)
              .isNotNull()
              .hasPayloadSize(
                      TestChannelSupport.DATA_CONTENT.getBytes(CharsetUtil.UTF_8).length
                              + TestChannelSupport.METADATA_CONTENT.getBytes(CharsetUtil.UTF_8)
                              .length)
              .hasMetadata(TestChannelSupport.METADATA_CONTENT)
              .hasData(TestChannelSupport.DATA_CONTENT)
              .hasNoFragmentsFollow()
              .typeOf(scenario.requestType())
              .hasClientSideStreamId()
              .hasStreamId(1)
              .hasNoLeaks();

      RaceTestUtils.race(
              ((Subscription) requestOperator)::cancel,
              () -> ((RequesterFrameHandler) requestOperator).handlePayload(response));

      assertSubscriber.values().forEach(Payload::release);
      Assertions.assertThat(response.refCnt()).isZero();

      activeStreams.assertNoActiveStreams();
      final boolean isEmpty = activeStreams.getDuplexConnection().isEmpty();
      if (!isEmpty) {
        final ByteBuf cancellationFrame = activeStreams.getDuplexConnection().awaitFrame();
        FrameAssert.assertThat(cancellationFrame)
                .isNotNull()
                .typeOf(FrameType.CANCEL)
                .hasClientSideStreamId()
                .hasStreamId(1)
                .hasNoLeaks();

        testRequestInterceptor
                .expectOnStart(1, scenario.requestType())
                .expectOnCancel(1)
                .expectNothing();
      }
      else {
        assertSubscriber.assertTerminated();
        testRequestInterceptor
                .expectOnStart(1, scenario.requestType())
                .expectOnComplete(1)
                .expectNothing();
      }
      Assertions.assertThat(activeStreams.getDuplexConnection().isEmpty()).isTrue();
      activeStreams.getAllocator().assertHasNoLeaks();
    }
  }

  /**
   * Ensures that in case we have element reassembling and then it happens the remote sends
   * (errorFrame) and downstream subscriber sends cancel() and we have racing between onError and
   * cancel we will not have any memory leaks
   */
  @ParameterizedTest
  @MethodSource("scenarios")
  public void shouldHaveNoUnexpectedErrorDuringOnErrorAndCancelRacing(Scenario scenario) {
    Assumptions.assumeThat(scenario.requestType())
            .isIn(REQUEST_RESPONSE, REQUEST_STREAM, REQUEST_CHANNEL);
    boolean[] withReassemblyOptions = new boolean[] { true, false };
    final ArrayList<Throwable> droppedErrors = new ArrayList<>();
    Hooks.onErrorDropped(droppedErrors::add);

    try {
      for (boolean withReassembly : withReassemblyOptions) {
        for (int i = 0; i < RaceTestConstants.REPEATS; i++) {
          final TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();
          final TestChannelSupport activeStreams =
                  TestChannelSupport.client(testRequestInterceptor);
          final Supplier<Payload> payloadSupplier =
                  () -> TestChannelSupport.genericPayload(activeStreams.getAllocator());

          final Publisher<?> requestOperator =
                  scenario.requestOperator(payloadSupplier, activeStreams);

          final StateAssert<?> stateAssert;
          if (requestOperator instanceof RequestResponseRequesterMono) {
            stateAssert = StateAssert.assertThat((RequestResponseRequesterMono) requestOperator);
          }
          else if (requestOperator instanceof RequestStreamRequesterFlux) {
            stateAssert = StateAssert.assertThat((RequestStreamRequesterFlux) requestOperator);
          }
          else {
            stateAssert = StateAssert.assertThat((RequestChannelRequesterFlux) requestOperator);
          }

          stateAssert.isUnsubscribed();
          final AssertSubscriber<Payload> assertSubscriber = AssertSubscriber.create(0);

          requestOperator.subscribe((AssertSubscriber) assertSubscriber);

          stateAssert.hasSubscribedFlagOnly();

          assertSubscriber.request(1);

          stateAssert.hasSubscribedFlag().hasRequestN(1).hasFirstFrameSentFlag();

          final ByteBuf sentFrame = activeStreams.getDuplexConnection().awaitFrame();
          FrameAssert.assertThat(sentFrame)
                  .isNotNull()
                  .hasPayloadSize(
                          TestChannelSupport.DATA_CONTENT.getBytes(CharsetUtil.UTF_8).length
                                  + TestChannelSupport.METADATA_CONTENT.getBytes(CharsetUtil.UTF_8)
                                  .length)
                  .hasMetadata(TestChannelSupport.METADATA_CONTENT)
                  .hasData(TestChannelSupport.DATA_CONTENT)
                  .hasNoFragmentsFollow()
                  .typeOf(scenario.requestType())
                  .hasClientSideStreamId()
                  .hasStreamId(1)
                  .hasNoLeaks();

          if (withReassembly) {
            final ByteBuf fragmentBuf =
                    activeStreams.getAllocator().buffer().writeBytes(new byte[] { 1, 2, 3 });
            ((RequesterFrameHandler) requestOperator).handleNext(fragmentBuf, true, false);
            // mimic frameHandler behaviour
            fragmentBuf.release();
          }

          final RuntimeException testException = new RuntimeException("test");
          RaceTestUtils.race(
                  ((Subscription) requestOperator)::cancel,
                  () -> ((RequesterFrameHandler) requestOperator).handleError(testException));

          activeStreams.assertNoActiveStreams();
          stateAssert.isTerminated();

          final boolean isEmpty = activeStreams.getDuplexConnection().isEmpty();
          if (!isEmpty) {
            final ByteBuf cancellationFrame = activeStreams.getDuplexConnection().awaitFrame();
            FrameAssert.assertThat(cancellationFrame)
                    .isNotNull()
                    .typeOf(FrameType.CANCEL)
                    .hasClientSideStreamId()
                    .hasStreamId(1)
                    .hasNoLeaks();

            Assertions.assertThat(droppedErrors).containsExactly(testException);
            testRequestInterceptor
                    .expectOnStart(1, scenario.requestType())
                    .expectOnCancel(1)
                    .expectNothing();
          }
          else {
            testRequestInterceptor
                    .expectOnStart(1, scenario.requestType())
                    .expectOnError(1)
                    .expectNothing();

            assertSubscriber.assertTerminated().assertErrorMessage("test");
          }
          Assertions.assertThat(activeStreams.getDuplexConnection().isEmpty()).isTrue();

          stateAssert.isTerminated();
          droppedErrors.clear();
          activeStreams.getAllocator().assertHasNoLeaks();
        }
      }
    }
    finally {
      Hooks.resetOnErrorDropped();
    }
  }

  /**
   * Ensures that in case of racing between first request and cancel does not going to introduce
   * leaks. <br>
   * <br>
   *
   * <p>Please note, first request may or may not happen so in case it happened before cancellation
   * signal we have to observe
   *
   * <ul>
   *   <li>RequestResponseFrame
   *   <li>CancellationFrame
   * </ul>
   *
   * <p>exactly in that order
   *
   * <p>Ensures full serialization of outgoing signal (frames)
   */
  @ParameterizedTest
  @MethodSource("scenarios")
  public void shouldBeConsistentInCaseOfRacingOfCancellationAndRequest(Scenario scenario) {
    Assumptions.assumeThat(scenario.requestType())
            .isIn(REQUEST_RESPONSE, REQUEST_STREAM, REQUEST_CHANNEL);
    for (int i = 0; i < RaceTestConstants.REPEATS; i++) {
      final TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();
      final TestChannelSupport activeStreams =
              TestChannelSupport.client(testRequestInterceptor);
      final Supplier<Payload> payloadSupplier =
              () -> TestChannelSupport.genericPayload(activeStreams.getAllocator());

      final Publisher<?> requestOperator = scenario.requestOperator(payloadSupplier, activeStreams);

      Payload response = ByteBufPayload.create("test", "test");

      final AssertSubscriber<Payload> assertSubscriber = new AssertSubscriber<>(0);

      requestOperator.subscribe((AssertSubscriber) assertSubscriber);

      RaceTestUtils.race(() -> assertSubscriber.cancel(), () -> assertSubscriber.request(1));

      if (!activeStreams.getDuplexConnection().isEmpty()) {
        final ByteBuf sentFrame = activeStreams.getDuplexConnection().awaitFrame();
        FrameAssert.assertThat(sentFrame)
                .isNotNull()
                .typeOf(scenario.requestType())
                .hasMetadata(TestChannelSupport.METADATA_CONTENT)
                .hasData(TestChannelSupport.DATA_CONTENT)
                .hasNoFragmentsFollow()
                .hasClientSideStreamId()
                .hasStreamId(1)
                .hasNoLeaks();

        final ByteBuf cancelFrame = activeStreams.getDuplexConnection().awaitFrame();
        FrameAssert.assertThat(cancelFrame)
                .isNotNull()
                .typeOf(FrameType.CANCEL)
                .hasClientSideStreamId()
                .hasStreamId(1)
                .hasNoLeaks();

        testRequestInterceptor
                .expectOnStart(1, scenario.requestType())
                .expectOnCancel(1)
                .expectNothing();
      }

      ((RequesterFrameHandler) requestOperator).handlePayload(response);
      assertSubscriber.values().forEach(Payload::release);

      Assertions.assertThat(response.refCnt()).isZero();
      activeStreams.assertNoActiveStreams();
      Assertions.assertThat(activeStreams.getDuplexConnection().isEmpty()).isTrue();
      activeStreams.getAllocator().assertHasNoLeaks();
    }
  }

  /** Ensures that CancelFrame is sent exactly once in case of racing between cancel() methods */
  @ParameterizedTest
  @MethodSource("scenarios")
  public void shouldSentCancelFrameExactlyOnce(Scenario scenario) {
    Assumptions.assumeThat(scenario.requestType())
            .isIn(REQUEST_RESPONSE, REQUEST_STREAM, REQUEST_CHANNEL);
    for (int i = 0; i < RaceTestConstants.REPEATS; i++) {
      final TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();
      final TestChannelSupport activeStreams =
              TestChannelSupport.client(testRequestInterceptor);
      final Supplier<Payload> payloadSupplier =
              () -> TestChannelSupport.genericPayload(activeStreams.getAllocator());

      final Publisher<?> requesterOperator =
              scenario.requestOperator(payloadSupplier, activeStreams);

      Payload response = ByteBufPayload.create("test", "test");

      final AssertSubscriber<Payload> assertSubscriber = new AssertSubscriber<>(0);

      requesterOperator.subscribe((AssertSubscriber) assertSubscriber);

      assertSubscriber.request(1);

      final ByteBuf sentFrame = activeStreams.getDuplexConnection().awaitFrame();
      FrameAssert.assertThat(sentFrame)
              .isNotNull()
              .hasNoFragmentsFollow()
              .typeOf(scenario.requestType())
              .hasClientSideStreamId()
              .hasMetadata(TestChannelSupport.METADATA_CONTENT)
              .hasData(TestChannelSupport.DATA_CONTENT)
              .hasStreamId(1)
              .hasNoLeaks();

      RaceTestUtils.race(
              ((Subscription) requesterOperator)::cancel, ((Subscription) requesterOperator)::cancel);

      final ByteBuf cancelFrame = activeStreams.getDuplexConnection().awaitFrame();
      FrameAssert.assertThat(cancelFrame)
              .isNotNull()
              .typeOf(FrameType.CANCEL)
              .hasClientSideStreamId()
              .hasStreamId(1)
              .hasNoLeaks();

      testRequestInterceptor
              .expectOnStart(1, scenario.requestType())
              .expectOnCancel(1)
              .expectNothing();

      activeStreams.assertNoActiveStreams();

      ((RequesterFrameHandler) requesterOperator).handlePayload(response);
      assertSubscriber.values().forEach(Payload::release);
      Assertions.assertThat(response.refCnt()).isZero();

      ((RequesterFrameHandler) requesterOperator).handleComplete();
      assertSubscriber.assertNotTerminated();

      activeStreams.assertNoActiveStreams();
      Assertions.assertThat(activeStreams.getDuplexConnection().isEmpty()).isTrue();
      activeStreams.getAllocator().assertHasNoLeaks();
    }
  }
}
