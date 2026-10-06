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

package infra.cloud.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies stable result handles and stream lifecycle without extra subscriptions.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2026/10/6
 */
class InvocationResultsTests {

  @Test
  void singleStartsOnceAndObserversDoNotStartWork() {
    var calls = new AtomicInteger();
    var result = InvocationResults.single(Mono.fromSupplier(() -> calls.incrementAndGet()));
    assertThat(result.value()).isSameAs(result.value());
    assertThat(result.completion()).isSameAs(result.completion());
    assertThat(calls).hasValue(0);
    result.start();
    result.start();
    assertThat(result.value().join()).isEqualTo(1);
    assertThat(result.completion().isSuccess()).isTrue();
    assertThat(calls).hasValue(1);
  }

  @Test
  void emptyFailureAndCancelBeforeStart() {
    var empty = InvocationResults.success(null);
    empty.start();
    assertThat(empty.value().join()).isNull();
    assertThat(empty.completion().isSuccess()).isTrue();
    var error = new IllegalStateException("failed");
    var failed = InvocationResults.failure(error);
    failed.start();
    assertThat(failed.value().getCause()).isSameAs(error);
    assertThat(failed.getException()).isSameAs(error);
    var calls = new AtomicInteger();
    var cancelled = InvocationResults.single(Mono.fromSupplier(() -> calls.incrementAndGet()));
    cancelled.value().cancel();
    cancelled.start();
    assertThat(cancelled.completion().isCancelled()).isTrue();
    assertThat(calls).hasValue(0);
  }

  @Test
  void streamDemandAndCompletionUseOneSubscription() {
    var calls = new AtomicInteger();
    var result = InvocationResults.stream(InvocationType.RESPONSE_STREAMING,
            Flux.<Object>just(1, 2).doOnSubscribe(s -> calls.incrementAndGet()));
    result.completion();
    result.start();
    assertThat(calls).hasValue(0);
    StepVerifier.create(result.values(), 0).thenRequest(1).expectNext(1)
            .then(() -> assertThat(result.completion().isDone()).isFalse())
            .thenRequest(1).expectNext(2).verifyComplete();
    assertThat(result.completion().isSuccess()).isTrue();
    StepVerifier.create(result.values()).expectError(IllegalStateException.class).verify();
    assertThat(calls).hasValue(1);
  }

  @Test
  void cancellationReachesSourceExactlyOnce() {
    var cancellations = new AtomicInteger();
    var result = InvocationResults.single(Mono.<Object>never().doOnCancel(cancellations::incrementAndGet));
    result.start();
    result.cancel();
    result.cancel();
    assertThat(cancellations).hasValue(1);
    assertThat(result.value().isCancelled()).isTrue();
  }
}
