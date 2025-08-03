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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import io.netty.util.IllegalReferenceCountException;
import infra.remoting.FrameAssert;
import infra.remoting.Payload;
import infra.remoting.buffer.LeaksTrackingByteBufAllocator;
import infra.remoting.frame.FrameType;
import infra.remoting.plugins.TestRequestInterceptor;
import infra.remoting.test.util.TestDuplexConnection;
import infra.remoting.util.ByteBufPayload;
import reactor.core.Exceptions;
import reactor.core.Scannable;
import reactor.test.StepVerifier;
import reactor.test.util.RaceTestUtils;

import static infra.remoting.core.FragmentationUtils.FRAME_OFFSET;
import static infra.remoting.core.FragmentationUtils.FRAME_OFFSET_WITH_METADATA;
import static infra.remoting.core.PayloadValidationUtils.INVALID_PAYLOAD_ERROR_MESSAGE;
import static infra.remoting.core.TestChannelSupport.genericPayload;
import static infra.remoting.frame.FrameLengthCodec.FRAME_LENGTH_MASK;

public class FireAndForgetRequesterMonoTest {

  @BeforeAll
  public static void setUp() {
    StepVerifier.setDefaultTimeout(Duration.ofSeconds(2));
  }

  /**
   * General StateMachine transition test. No Fragmentation enabled In this test we check that the
   * given instance of FireAndForgetMono subscribes, and then sends frame immediately
   */
  @ParameterizedTest
  @MethodSource("frameSent")
  public void frameShouldBeSentOnSubscription(Consumer<FireAndForgetRequesterMono> monoConsumer) {
    final TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();
    final TestChannelSupport activeStreams =
            TestChannelSupport.client(testRequestInterceptor);
    final Payload payload = genericPayload(activeStreams.getAllocator());
    final FireAndForgetRequesterMono fireAndForgetRequesterMono =
            new FireAndForgetRequesterMono(payload, activeStreams);
    final StateAssert<FireAndForgetRequesterMono> stateAssert =
            StateAssert.assertThat(FireAndForgetRequesterMono.STATE, fireAndForgetRequesterMono);

    stateAssert.isUnsubscribed();
    activeStreams.assertNoActiveStreams();

    monoConsumer.accept(fireAndForgetRequesterMono);

    Assertions.assertThat(payload.refCnt()).isZero();
    // should not add anything to map
    stateAssert.isTerminated();
    activeStreams.assertNoActiveStreams();
    final ByteBuf frame = activeStreams.getDuplexConnection().awaitFrame();
    FrameAssert.assertThat(frame)
            .isNotNull()
            .hasPayloadSize(
                    "testData".getBytes(CharsetUtil.UTF_8).length
                            + "testMetadata".getBytes(CharsetUtil.UTF_8).length)
            .hasMetadata("testMetadata")
            .hasData("testData")
            .hasNoFragmentsFollow()
            .typeOf(FrameType.REQUEST_FNF)
            .hasClientSideStreamId()
            .hasStreamId(1)
            .hasNoLeaks();

    Assertions.assertThat(activeStreams.getDuplexConnection().isEmpty()).isTrue();
    activeStreams.getAllocator().assertHasNoLeaks();
    testRequestInterceptor
            .expectOnStart(1, FrameType.REQUEST_FNF)
            .expectOnComplete(1)
            .expectNothing();
  }

  /**
   * General StateMachine transition test. Fragmentation enabled In this test we check that the
   * given instance of FireAndForgetMono subscribes, and then sends all fragments as a separate
   * frame immediately
   */
  @ParameterizedTest
  @MethodSource("frameSent")
  public void frameFragmentsShouldBeSentOnSubscription(
          Consumer<FireAndForgetRequesterMono> monoConsumer) {
    final int mtu = 64;
    final TestChannelSupport streamManager = TestChannelSupport.client(mtu);
    final LeaksTrackingByteBufAllocator allocator = streamManager.getAllocator();
    final TestDuplexConnection sender = streamManager.getDuplexConnection();

    final byte[] metadata = new byte[65];
    final byte[] data = new byte[129];
    ThreadLocalRandom.current().nextBytes(metadata);
    ThreadLocalRandom.current().nextBytes(data);

    final Payload payload = ByteBufPayload.create(data, metadata);

    final FireAndForgetRequesterMono fireAndForgetRequesterMono =
            new FireAndForgetRequesterMono(payload, streamManager);
    final StateAssert<FireAndForgetRequesterMono> stateAssert =
            StateAssert.assertThat(FireAndForgetRequesterMono.STATE, fireAndForgetRequesterMono);

    stateAssert.isUnsubscribed();
    streamManager.assertNoActiveStreams();

    monoConsumer.accept(fireAndForgetRequesterMono);

    // should not add anything to map
    streamManager.assertNoActiveStreams();
    stateAssert.isTerminated();

    Assertions.assertThat(payload.refCnt()).isZero();

    final ByteBuf frameFragment1 = sender.awaitFrame();
    FrameAssert.assertThat(frameFragment1)
            .isNotNull()
            .hasPayloadSize(
                    64 - FRAME_OFFSET_WITH_METADATA) // 64 - 6 (frame headers) - 3 (encoded metadata
            // length) - 3 frame length
            .hasMetadata(Arrays.copyOf(metadata, 52))
            .hasData(Unpooled.EMPTY_BUFFER)
            .hasFragmentsFollow()
            .typeOf(FrameType.REQUEST_FNF)
            .hasClientSideStreamId()
            .hasStreamId(1)
            .hasNoLeaks();

    final ByteBuf frameFragment2 = sender.awaitFrame();
    FrameAssert.assertThat(frameFragment2)
            .isNotNull()
            .hasPayloadSize(
                    64 - FRAME_OFFSET_WITH_METADATA) // 64 - 6 (frame headers) - 3 (encoded metadata
            // length) - 3 frame length
            .hasMetadata(Arrays.copyOfRange(metadata, 52, 65))
            .hasData(Arrays.copyOf(data, 39))
            .hasFragmentsFollow()
            .typeOf(FrameType.NEXT)
            .hasClientSideStreamId()
            .hasStreamId(1)
            .hasNoLeaks();

    final ByteBuf frameFragment3 = sender.awaitFrame();
    FrameAssert.assertThat(frameFragment3)
            .isNotNull()
            .hasPayloadSize(
                    64 - FRAME_OFFSET) // 64 - 6 (frame headers) - 3 frame length (no metadata - no length)
            .hasNoMetadata()
            .hasData(Arrays.copyOfRange(data, 39, 94))
            .hasFragmentsFollow()
            .typeOf(FrameType.NEXT)
            .hasClientSideStreamId()
            .hasStreamId(1)
            .hasNoLeaks();

    final ByteBuf frameFragment4 = sender.awaitFrame();
    FrameAssert.assertThat(frameFragment4)
            .isNotNull()
            .hasPayloadSize(35)
            .hasNoMetadata()
            .hasData(Arrays.copyOfRange(data, 94, 129))
            .hasNoFragmentsFollow()
            .typeOf(FrameType.NEXT)
            .hasClientSideStreamId()
            .hasStreamId(1)
            .hasNoLeaks();

    Assertions.assertThat(sender.isEmpty()).isTrue();
    allocator.assertHasNoLeaks();
  }

  static Stream<Consumer<FireAndForgetRequesterMono>> frameSent() {
    return Stream.of(
            (s) -> StepVerifier.create(s).expectSubscription().expectComplete().verify(),
            FireAndForgetRequesterMono::block);
  }

  /**
   * RefCnt validation test. Should send error if RefCnt is incorrect and frame has already been
   * released Note: ONCE state should be 0
   */
  @ParameterizedTest
  @MethodSource("shouldErrorOnIncorrectRefCntInGivenPayloadSource")
  public void shouldErrorOnIncorrectRefCntInGivenPayload(
          Consumer<FireAndForgetRequesterMono> monoConsumer) {
    final TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();
    final TestChannelSupport streamManager =
            TestChannelSupport.client(testRequestInterceptor);
    final LeaksTrackingByteBufAllocator allocator = streamManager.getAllocator();
    final TestDuplexConnection sender = streamManager.getDuplexConnection();
    final Payload payload = ByteBufPayload.create("");
    payload.release();

    final FireAndForgetRequesterMono fireAndForgetRequesterMono =
            new FireAndForgetRequesterMono(payload, streamManager);
    final StateAssert<FireAndForgetRequesterMono> stateAssert =
            StateAssert.assertThat(FireAndForgetRequesterMono.STATE, fireAndForgetRequesterMono);

    stateAssert.isUnsubscribed();
    streamManager.assertNoActiveStreams();

    monoConsumer.accept(fireAndForgetRequesterMono);

    stateAssert.isTerminated();
    streamManager.assertNoActiveStreams();

    Assertions.assertThat(sender.isEmpty()).isTrue();
    allocator.assertHasNoLeaks();
    testRequestInterceptor
            .expectOnReject(FrameType.REQUEST_FNF, new IllegalReferenceCountException("refCnt: 0"))
            .expectNothing();
  }

  static Stream<Consumer<FireAndForgetRequesterMono>>
  shouldErrorOnIncorrectRefCntInGivenPayloadSource() {
    return Stream.of(
            (s) ->
                    StepVerifier.create(s)
                            .expectSubscription()
                            .expectError(IllegalReferenceCountException.class)
                            .verify(),
            fireAndForgetRequesterMono ->
                    Assertions.assertThatThrownBy(fireAndForgetRequesterMono::block)
                            .isInstanceOf(IllegalReferenceCountException.class));
  }

  /**
   * Check that proper payload size validation is enabled so in case payload fragmentation is
   * disabled we will not send anything bigger that 16MB (see specification for MAX frame size)
   */
  @ParameterizedTest
  @MethodSource("shouldErrorIfFragmentExitsAllowanceIfFragmentationDisabledSource")
  public void shouldErrorIfFragmentExitsAllowanceIfFragmentationDisabled(
          Consumer<FireAndForgetRequesterMono> monoConsumer) {
    final TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();
    final TestChannelSupport streamManager =
            TestChannelSupport.client(testRequestInterceptor);
    final LeaksTrackingByteBufAllocator allocator = streamManager.getAllocator();
    final TestDuplexConnection sender = streamManager.getDuplexConnection();

    final byte[] metadata = new byte[FRAME_LENGTH_MASK];
    final byte[] data = new byte[FRAME_LENGTH_MASK];
    ThreadLocalRandom.current().nextBytes(metadata);
    ThreadLocalRandom.current().nextBytes(data);

    final Payload payload = ByteBufPayload.create(data, metadata);

    final FireAndForgetRequesterMono fireAndForgetRequesterMono =
            new FireAndForgetRequesterMono(payload, streamManager);
    final StateAssert<FireAndForgetRequesterMono> stateAssert =
            StateAssert.assertThat(FireAndForgetRequesterMono.STATE, fireAndForgetRequesterMono);

    stateAssert.isUnsubscribed();
    streamManager.assertNoActiveStreams();

    monoConsumer.accept(fireAndForgetRequesterMono);

    Assertions.assertThat(payload.refCnt()).isZero();

    stateAssert.isTerminated();
    streamManager.assertNoActiveStreams();
    Assertions.assertThat(sender.isEmpty()).isTrue();
    allocator.assertHasNoLeaks();
    testRequestInterceptor
            .expectOnReject(
                    FrameType.REQUEST_FNF,
                    new IllegalArgumentException(
                            String.format(INVALID_PAYLOAD_ERROR_MESSAGE, FRAME_LENGTH_MASK)))
            .expectNothing();
  }

  static Stream<Consumer<FireAndForgetRequesterMono>>
  shouldErrorIfFragmentExitsAllowanceIfFragmentationDisabledSource() {
    return Stream.of(
            (s) ->
                    StepVerifier.create(s)
                            .expectSubscription()
                            .consumeErrorWith(
                                    t ->
                                            Assertions.assertThat(t)
                                                    .hasMessage(
                                                            String.format(INVALID_PAYLOAD_ERROR_MESSAGE, FRAME_LENGTH_MASK))
                                                    .isInstanceOf(IllegalArgumentException.class))
                            .verify(),
            fireAndForgetRequesterMono ->
                    Assertions.assertThatThrownBy(fireAndForgetRequesterMono::block)
                            .hasMessage(String.format(INVALID_PAYLOAD_ERROR_MESSAGE, FRAME_LENGTH_MASK))
                            .isInstanceOf(IllegalArgumentException.class));
  }

  /**
   * Ensures that frame will not be sent if we dont have availability for that. Options: 1. Channel
   * disposed / Connection Error, so all racing on existing interactions should be terminated as
   * well 2. Channel tries to use lease and end-ups with no available leases
   */
  @ParameterizedTest
  @MethodSource("shouldErrorIfNoAvailabilitySource")
  public void shouldErrorIfNoAvailability(Consumer<FireAndForgetRequesterMono> monoConsumer) {
    final TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();
    final RuntimeException exception = new RuntimeException("test");
    final TestChannelSupport streamManager =
            TestChannelSupport.client(exception, testRequestInterceptor);
    final LeaksTrackingByteBufAllocator allocator = streamManager.getAllocator();
    final TestDuplexConnection sender = streamManager.getDuplexConnection();
    final Payload payload = genericPayload(allocator);

    final FireAndForgetRequesterMono fireAndForgetRequesterMono =
            new FireAndForgetRequesterMono(payload, streamManager);
    final StateAssert<FireAndForgetRequesterMono> stateAssert =
            StateAssert.assertThat(FireAndForgetRequesterMono.STATE, fireAndForgetRequesterMono);

    stateAssert.isUnsubscribed();
    streamManager.assertNoActiveStreams();

    monoConsumer.accept(fireAndForgetRequesterMono);

    Assertions.assertThat(payload.refCnt()).isZero();

    stateAssert.isTerminated();
    streamManager.assertNoActiveStreams();
    Assertions.assertThat(sender.isEmpty()).isTrue();
    allocator.assertHasNoLeaks();
    testRequestInterceptor.expectOnReject(FrameType.REQUEST_FNF, exception).expectNothing();
  }

  static Stream<Consumer<FireAndForgetRequesterMono>> shouldErrorIfNoAvailabilitySource() {
    return Stream.of(
            (s) ->
                    StepVerifier.create(s)
                            .expectSubscription()
                            .consumeErrorWith(
                                    t ->
                                            Assertions.assertThat(t)
                                                    .hasMessage("test")
                                                    .isInstanceOf(RuntimeException.class))
                            .verify(),
            fireAndForgetRequesterMono ->
                    Assertions.assertThatThrownBy(fireAndForgetRequesterMono::block)
                            .hasMessage("test")
                            .isInstanceOf(RuntimeException.class));
  }

  /** Ensures single subscription happens in case of racing */
  @Test
  public void shouldSubscribeExactlyOnce1() {
    final TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();
    final TestChannelSupport streamManager =
            TestChannelSupport.client(testRequestInterceptor);
    final LeaksTrackingByteBufAllocator allocator = streamManager.getAllocator();
    final TestDuplexConnection sender = streamManager.getDuplexConnection();

    for (int i = 1; i < 50000; i += 2) {
      final Payload payload = ByteBufPayload.create("testData", "testMetadata");

      final FireAndForgetRequesterMono fireAndForgetRequesterMono =
              new FireAndForgetRequesterMono(payload, streamManager);
      final StateAssert<FireAndForgetRequesterMono> stateAssert =
              StateAssert.assertThat(FireAndForgetRequesterMono.STATE, fireAndForgetRequesterMono);

      Assertions.assertThatThrownBy(
                      () ->
                              RaceTestUtils.race(
                                      () -> {
                                        AtomicReference<Throwable> atomicReference = new AtomicReference<>();
                                        fireAndForgetRequesterMono.subscribe(null, atomicReference::set);
                                        Throwable throwable = atomicReference.get();
                                        if (throwable != null) {
                                          throw Exceptions.propagate(throwable);
                                        }
                                      },
                                      fireAndForgetRequesterMono::block))
              .matches(
                      t -> {
                        Assertions.assertThat(t)
                                .hasMessageContaining("FireAndForgetMono allows only a single Subscriber");
                        return true;
                      });

      final ByteBuf frame = sender.awaitFrame();
      FrameAssert.assertThat(frame)
              .isNotNull()
              .hasPayloadSize(
                      "testData".getBytes(CharsetUtil.UTF_8).length
                              + "testMetadata".getBytes(CharsetUtil.UTF_8).length)
              .hasMetadata("testMetadata")
              .hasData("testData")
              .hasNoFragmentsFollow()
              .typeOf(FrameType.REQUEST_FNF)
              .hasClientSideStreamId()
              .hasStreamId(i)
              .hasNoLeaks();

      stateAssert.isTerminated();
      streamManager.assertNoActiveStreams();
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

    Assertions.assertThat(sender.isEmpty()).isTrue();
    allocator.assertHasNoLeaks();
  }

  @Test
  public void checkName() {
    final TestChannelSupport testRequesterResponderSupport =
            TestChannelSupport.client();
    final LeaksTrackingByteBufAllocator allocator = testRequesterResponderSupport.getAllocator();
    final Payload payload = ByteBufPayload.create("testData", "testMetadata");

    final FireAndForgetRequesterMono fireAndForgetRequesterMono =
            new FireAndForgetRequesterMono(payload, testRequesterResponderSupport);

    Assertions.assertThat(Scannable.from(fireAndForgetRequesterMono).name())
            .isEqualTo("source(FireAndForgetMono)");
    allocator.assertHasNoLeaks();
  }
}
