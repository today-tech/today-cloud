package infra.cloud.provider;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.Duration;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import infra.cloud.service.InvocationResults;

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
    when(method.handleReturnValue(any(), any())).thenAnswer(i -> InvocationResults.success(i.getArgument(1)));
    return new RemoteRequest(method, new Object[] { "original" }, mock(ServiceObject.class));
  }

  @Test
  void orderedAroundChainSharesOriginalRequestAndExecutesOnce() {
    var events = new ArrayList<String>();
    var calls = new AtomicInteger();
    var request = request(calls);
    request.setAttribute("seed", "initial");
    ServiceInterceptor first = (invocation, chain) -> {
      assertThat(invocation).isSameAs(request);
      assertThat(invocation.getAttribute("seed")).isEqualTo("initial");
      assertThat(invocation.getAttribute("key")).isNull();
      invocation.setAttribute("key", "shared");
      invocation.getArguments()[0] = "changed";
      events.add("first-before");
      return InvocationResults.single(Mono.from(InvocationResults.publisher(chain.proceed(invocation))).doOnNext(v -> events.add("first-after")));
    };
    ServiceInterceptor second = (invocation, chain) -> {
      assertThat(invocation.getAttribute("key")).isEqualTo("shared");
      events.add("second-before");
      return InvocationResults.single(Mono.from(InvocationResults.publisher(chain.proceed(invocation))).doOnNext(v -> events.add("second-after")));
    };
    var executor = new DefaultServiceRequestExecutor(List.of(first, second), Schedulers.immediate());
    var result = executor.execute(request);
    assertThat(events).isEmpty();
    StepVerifier.create(InvocationResults.publisher(result)).expectNext("changed").verifyComplete();
    assertThat(events).containsExactly("first-before", "second-before", "second-after", "first-after");
    StepVerifier.create(InvocationResults.publisher(result)).expectNext("changed").verifyComplete();
    assertThat(calls).hasValue(1);
    assertThat(request.getArguments()).containsExactly("changed");
    assertThat(request.getAttribute("key")).isEqualTo("shared");
  }

  @Test
  void shortCircuitDoesNotInvokeService() {
    var calls = new AtomicInteger();
    var executor = new DefaultServiceRequestExecutor(List.of((invocation, chain) -> InvocationResults.success("cached")), Schedulers.immediate());
    StepVerifier.create(InvocationResults.publisher(executor.execute(request(calls)))).expectNext("cached").verifyComplete();
    assertThat(calls).hasValue(0);
  }

  @Test
  void unusedProceedDoesNotConsumeChainAndResultObservationDoesNotRepeatExecution() throws Exception {
    var calls = new AtomicInteger();
    var request = request(calls);
    ServiceInterceptor interceptor = (candidate, continuation) -> {
      continuation.proceed(candidate); // No subscription, hence no service execution.
      return continuation.proceed(candidate);
    };
    var chain = new DefaultInterceptorChain(List.of(interceptor));
    var result = chain.proceed(request);
    StepVerifier.create(InvocationResults.publisher(result)).expectNext("original").verifyComplete();
    StepVerifier.create(InvocationResults.publisher(result)).expectNext("original").verifyComplete();
    assertThat(calls).hasValue(1);
  }

  @Test
  void immutableChainIsReusableAcrossConcurrentRequests() {
    var interceptors = new ArrayList<ServiceInterceptor>();
    var interceptions = new AtomicInteger();
    interceptors.add((request, chain) -> {
      interceptions.incrementAndGet();
      return chain.proceed(request);
    });
    var chain = new DefaultInterceptorChain(interceptors);
    interceptors.clear();
    var calls = new AtomicInteger();
    StepVerifier.create(reactor.core.publisher.Flux.range(0, 50)
             .flatMap(i -> {
               try {
                 return Mono.from(InvocationResults.publisher(chain.proceed(request(calls))))
                         .subscribeOn(Schedulers.parallel());
               }
               catch (Exception error) {
                 return Mono.error(error);
               }
             }))
            .expectNextCount(50).verifyComplete();
    assertThat(calls).hasValue(50);
    assertThat(interceptions).hasValue(50);
  }

  @Test
  void thrownInterceptorExceptionBecomesErrorSignal() {
    var calls = new AtomicInteger();
    var executor = new DefaultServiceRequestExecutor(List.of((invocation, chain) -> {
      throw new IllegalArgumentException("rejected");
    }), Schedulers.immediate());
    StepVerifier.create(InvocationResults.publisher(executor.execute(request(calls)))).expectErrorMessage("rejected").verify();
    assertThat(calls).hasValue(0);
  }

  @Test
  void cancellationAndAsyncErrorAreVisibleToInterceptor() {
    var cancelled = new AtomicInteger();
    ServiceInterceptor interceptor = (invocation, chain) -> InvocationResults.single(Mono.from(InvocationResults.publisher(chain.proceed(invocation))).doOnCancel(cancelled::incrementAndGet));
    var method = mock(InvocableMethod.class);
    when(method.invoke(any())).thenReturn("value");
    when(method.handleReturnValue(any(), any())).thenAnswer(i -> InvocationResults.single(Mono.never()));
    var request = new RemoteRequest(method, new Object[] { "value" }, mock(ServiceObject.class));
    var executor = new DefaultServiceRequestExecutor(List.of(interceptor), Schedulers.immediate());
    StepVerifier.create(InvocationResults.publisher(executor.execute(request))).thenAwait(Duration.ofMillis(20)).thenCancel().verify();
    assertThat(cancelled).hasValue(1);
    when(method.handleReturnValue(any(), any())).thenAnswer(i -> InvocationResults.failure(new IllegalStateException("async")));
    StepVerifier.create(InvocationResults.publisher(executor.execute(request))).expectErrorMessage("async").verify();
  }
}
