package infra.cloud.provider;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.Duration;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies ordering, short circuits and per-call continuation ownership.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2026/10/5
 */
class ServiceInterceptorTests {

  private RemoteRequest request(AtomicInteger calls) {
    var method = mock(InvocableMethod.class);
    when(method.invoke(any())).thenAnswer(i -> {
      calls.incrementAndGet();
      return ((Object[]) i.getArgument(0))[0];
    });
    when(method.handleReturnValue(any(), any())).thenAnswer(i -> Mono.just(i.getArgument(1)));
    return new RemoteRequest(method, new Object[] { "original" }, mock(ServiceObject.class));
  }

  @Test
  void orderedAroundChainAndIsolatedArgumentsAndAttributes() {
    var events = new ArrayList<String>();
    var calls = new AtomicInteger();
    var request = request(calls);
    ServiceInterceptor first = invocation -> {
      assertThat(invocation.getAttribute("key")).isNull();
      invocation.setAttribute("key", "shared");
      invocation.getArguments()[0] = "changed";
      events.add("first-before");
      return Mono.from(invocation.proceed()).doOnNext(v -> events.add("first-after"));
    };
    ServiceInterceptor second = invocation -> {
      assertThat(invocation.getAttribute("key")).isEqualTo("shared");
      events.add("second-before");
      return Mono.from(invocation.proceed()).doOnNext(v -> events.add("second-after"));
    };
    var executor = new DefaultServiceRequestExecutor(List.of(first, second), Schedulers.immediate());
    var result = executor.execute(request);
    assertThat(events).isEmpty();
    StepVerifier.create(result).expectNext("changed").verifyComplete();
    assertThat(events).containsExactly("first-before", "second-before", "second-after", "first-after");
    StepVerifier.create(result).expectNext("changed").verifyComplete();
    assertThat(calls).hasValue(2);
    assertThat(request.getArguments()).containsExactly("original");
  }

  @Test
  void shortCircuitDoesNotInvokeService() {
    var calls = new AtomicInteger();
    var executor = new DefaultServiceRequestExecutor(List.of(invocation -> Mono.just("cached")), Schedulers.immediate());
    StepVerifier.create(executor.execute(request(calls))).expectNext("cached").verifyComplete();
    assertThat(calls).hasValue(0);
  }

  @Test
  void duplicateProceedAndSubscriptionAreRejected() {
    var calls = new AtomicInteger();
    var invocation = new DefaultProviderInvocation(request(calls), List.of());
    var result = invocation.proceed();
    assertThatThrownBy(invocation::proceed).isInstanceOf(IllegalStateException.class);
    StepVerifier.create(result).expectNext("original").verifyComplete();
    StepVerifier.create(result).expectError(IllegalStateException.class).verify();
    assertThat(calls).hasValue(1);
  }

  @Test
  void thrownInterceptorExceptionBecomesErrorSignal() {
    var calls = new AtomicInteger();
    var executor = new DefaultServiceRequestExecutor(List.of(invocation -> {
      throw new IllegalArgumentException("rejected");
    }), Schedulers.immediate());
    StepVerifier.create(executor.execute(request(calls))).expectErrorMessage("rejected").verify();
    assertThat(calls).hasValue(0);
  }

  @Test
  void cancellationAndAsyncErrorAreVisibleToInterceptor() {
    var cancelled = new AtomicInteger();
    ServiceInterceptor interceptor = invocation -> Mono.from(invocation.proceed()).doOnCancel(cancelled::incrementAndGet);
    var method = mock(InvocableMethod.class);
    when(method.invoke(any())).thenReturn("value");
    when(method.handleReturnValue(any(), any())).thenReturn(Mono.never());
    var request = new RemoteRequest(method, new Object[] { "value" }, mock(ServiceObject.class));
    var executor = new DefaultServiceRequestExecutor(List.of(interceptor), Schedulers.immediate());
    StepVerifier.create(executor.execute(request)).thenAwait(Duration.ofMillis(20)).thenCancel().verify();
    assertThat(cancelled).hasValue(1);
    when(method.handleReturnValue(any(), any())).thenReturn(Mono.error(new IllegalStateException("async")));
    StepVerifier.create(executor.execute(request)).expectErrorMessage("async").verify();
  }
}
