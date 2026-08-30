/*
 * Copyright 2021 - 2026 the TODAY authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.remoting.test.util;

import org.reactivestreams.Publisher;

import java.util.concurrent.atomic.AtomicInteger;

import infra.remoting.Channel;
import infra.remoting.Payload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

public class MockChannel implements Channel {

  private final AtomicInteger fnfCount;
  private final AtomicInteger rrCount;
  private final AtomicInteger rStreamCount;
  private final AtomicInteger rSubCount;
  private final AtomicInteger rChannelCount;
  private final AtomicInteger pushCount;
  private final Channel delegate;

  public MockChannel(Channel delegate) {
    this.delegate = delegate;
    fnfCount = new AtomicInteger();
    rrCount = new AtomicInteger();
    rStreamCount = new AtomicInteger();
    rSubCount = new AtomicInteger();
    rChannelCount = new AtomicInteger();
    pushCount = new AtomicInteger();
  }

  @Override
  public final Mono<Void> fireAndForget(Payload payload) {
    return delegate.fireAndForget(payload).doOnSubscribe(s -> fnfCount.incrementAndGet());
  }

  @Override
  public final Mono<Payload> requestResponse(Payload payload) {
    return delegate.requestResponse(payload).doOnSubscribe(s -> rrCount.incrementAndGet());
  }

  @Override
  public final Flux<Payload> requestStream(Payload payload) {
    return delegate.requestStream(payload).doOnSubscribe(s -> rStreamCount.incrementAndGet());
  }

  @Override
  public final Flux<Payload> requestChannel(Publisher<Payload> payloads) {
    return delegate.requestChannel(payloads).doOnSubscribe(s -> rChannelCount.incrementAndGet());
  }

  @Override
  public final Mono<Void> metadataPush(Payload payload) {
    return delegate.metadataPush(payload).doOnSubscribe(s -> pushCount.incrementAndGet());
  }

  @Override
  public double availability() {
    return delegate.availability();
  }

  @Override
  public void dispose() {
    delegate.dispose();
  }

  @Override
  public boolean isDisposed() {
    return delegate.isDisposed();
  }

  @Override
  public Mono<Void> onClose() {
    return delegate.onClose();
  }

  public void assertFireAndForgetCount(int expected) {
    assertCount(expected, "fire-and-forget", fnfCount);
  }

  public void assertRequestResponseCount(int expected) {
    assertCount(expected, "request-response", rrCount);
  }

  public void assertRequestStreamCount(int expected) {
    assertCount(expected, "request-stream", rStreamCount);
  }

  public void assertRequestSubscriptionCount(int expected) {
    assertCount(expected, "request-subscription", rSubCount);
  }

  public void assertRequestChannelCount(int expected) {
    assertCount(expected, "request-channel", rChannelCount);
  }

  public void assertMetadataPushCount(int expected) {
    assertCount(expected, "metadata-push", pushCount);
  }

  private static void assertCount(int expected, String type, AtomicInteger counter) {
    assertThat(counter.get())
            .describedAs("Unexpected invocations for " + type + '.')
            .isEqualTo(expected);
  }
}
