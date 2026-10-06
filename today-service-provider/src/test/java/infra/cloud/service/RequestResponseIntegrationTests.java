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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import infra.cloud.ServiceTimeoutException;
import infra.cloud.provider.LocalServiceHolder;
import infra.cloud.provider.RequestDeserializer;
import infra.cloud.provider.ResponseSerializer;
import infra.cloud.provider.ServiceChannelHandler;
import infra.cloud.provider.ServiceObject;
import infra.cloud.serialize.ThrowableSerialization;
import infra.cloud.serialize.support.SerializableReturnValueSerialization;
import infra.cloud.service.serialize.RequestSerializer;
import infra.cloud.service.serialize.ResponseDeserializer;
import infra.core.ReactiveAdapterRegistry;
import infra.core.ReactiveTypeDescriptor;
import infra.remoting.ChannelAcceptor;
import infra.remoting.Closeable;
import infra.remoting.core.ChannelConnector;
import infra.remoting.core.RemotingClient;
import infra.remoting.core.RemotingServer;
import infra.remoting.frame.decoder.PayloadDecoder;
import infra.remoting.transport.local.LocalClientTransport;
import infra.remoting.transport.local.LocalServerTransport;
import infra.util.concurrent.Future;
import infra.util.concurrent.Promise;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Exercises the proxy, wire serialization and request-response protocol together.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2026/10/4
 */
class RequestResponseIntegrationTests {

  private Closeable server;
  private RemotingClient client;
  private ServiceMethodInvoker invoker;
  private TestService service;
  private final TestServiceImpl implementation = new TestServiceImpl();

  private final java.util.concurrent.atomic.AtomicReference<Metadata> received = new java.util.concurrent.atomic.AtomicReference<>();
  private final java.util.concurrent.atomic.AtomicReference<Metadata> outgoing = new java.util.concurrent.atomic.AtomicReference<>(Metadata.empty());

  @BeforeEach
  void setUp() {
    ServiceMetadataProvider metadata = type -> new ServiceMetadata("test", "1", List.of(type.getName()));
    var serialization = new SerializableReturnValueSerialization();
    var adapters = new ReactiveAdapterRegistry();
    adapters.registerReactiveType(ReactiveTypeDescriptor.singleOptionalValue(AsyncValue.class,
                    () -> new AsyncValue<>(Mono.empty())),
            value -> ((AsyncValue<?>) value).publisher(),
            publisher -> new AsyncValue<>(Mono.from(publisher)));
    var holder = mock(LocalServiceHolder.class);
    when(holder.getServiceObject(TestService.class.getName()))
            .thenReturn(new ServiceObject(TestService.class, implementation));
    var requests = new RequestDeserializer(List.of(),
            new infra.cloud.provider.DefaultServiceInterfaceMetadataProvider(metadata), holder, adapters);
    infra.cloud.provider.ServiceInterceptor capture = (request, chain) -> {
      received.set(request.getMetadata());
      assertThat(request.getAttribute("local-only")).isNull();
      return chain.proceed(request);
    };
    var handler = new ServiceChannelHandler(requests, new ResponseSerializer(List.of(serialization)),
            new infra.cloud.provider.DefaultServiceRequestExecutor(List.of(capture)));
    String name = UUID.randomUUID().toString();
    server = RemotingServer.create(ChannelAcceptor.with(handler)).payloadDecoder(PayloadDecoder.ZERO_COPY)
            .bindNow(LocalServerTransport.create(name));
    client = RemotingClient.from(ChannelConnector.create().payloadDecoder(PayloadDecoder.ZERO_COPY)
            .connect(LocalClientTransport.create(name)));
    ClientInterceptor inject = (invocation, chain) -> {
      invocation.setAttribute("local-only", "not transmitted");
      for (var entry : outgoing.get().entries()) {
        if (entry.binary()) {
          invocation.getMetadata().addBinary(entry.name(), entry.value());
        }
        else {
          invocation.getMetadata().add(entry.name(), new String(entry.value(), java.nio.charset.StandardCharsets.UTF_8));
        }
      }
      return chain.proceed(invocation);
    };
    invoker = new ServiceMethodInvoker(List.of(inject), method -> client, ByteBufAllocator.DEFAULT,
            new RequestSerializer(List.of()), new ResponseDeserializer(List.of(serialization), new ThrowableSerialization()));
    service = new DefaultServiceProxyFactory(new DefaultServiceInterfaceMetadataProvider(metadata, List.of(), adapters), invoker)
            .getService(TestService.class);
  }

  @AfterEach
  void tearDown() {
    if (client != null) {
      client.dispose();
    }
    if (server != null) {
      server.dispose();
    }
  }

  @Test
  void metadataCrossesWireAndRemainsReadableAfterPayloadRelease() {
    outgoing.set(new Metadata().add("traceparent", "00-0123456789abcdef0123456789abcdef-0123456789abcdef-01")
            .add("tenant-id", "tenant").addBinary("custom-bin", new byte[] { 3, 4 }));
    assertThat(service.value()).isEqualTo("value");
    var first = received.get();
    assertThat(first.get("tenant-id")).isEqualTo("tenant");
    assertThat(first.getBinary("custom-bin")).containsExactly((byte) 3, (byte) 4);
    assertThatThrownBy(() -> first.add("x", "y")).isInstanceOf(IllegalStateException.class);
    outgoing.set(Metadata.empty());
    assertThat(service.value()).isEqualTo("value");
    assertThat(received.get().isEmpty()).isTrue();
    assertThat(first.get("traceparent")).startsWith("00-");
  }

  @Test
  void ordinaryAndPrimitiveValues() {
    assertThat(service.value()).isEqualTo("value");
    assertThat(service.number()).isEqualTo(42);
  }

  @Test
  void nullAndVoidValues() {
    assertThat(service.nullValue()).isNull();
    service.nothing();
    assertThat(implementation.calls).hasValue(1);
  }

  @Test
  void monoIsLazyAndRepeatedObservationSharesOneInvocation() {
    Mono<String> result = service.mono();
    assertThat(implementation.calls).hasValue(0);
    StepVerifier.create(result).expectNext("mono").verifyComplete();
    StepVerifier.create(result).expectNext("mono").verifyComplete();
    assertThat(implementation.calls).hasValue(1);
  }

  @Test
  void emptyMono() {
    StepVerifier.create(service.empty()).verifyComplete();
    StepVerifier.create(service.monoVoid()).verifyComplete();
  }

  @Test
  void synchronousAndAsynchronousErrorsDoNotBreakConnection() {
    assertThatThrownBy(service::failure).hasMessageContaining("sync failure");
    StepVerifier.create(service.monoFailure()).expectErrorMatches(error -> error.getMessage().contains("async failure"))
            .verify(Duration.ofSeconds(5));
    StepVerifier.create(service.monoThrow()).expectErrorMatches(error -> error.getMessage().contains("sync mono failure"))
            .verify(Duration.ofSeconds(5));
    assertThat(service.value()).isEqualTo("value");
  }

  @Test
  void timeoutCancelsServerSubscription() throws Exception {
    // Establish the connection before measuring the per-request deadline.
    assertThat(service.value()).isEqualTo("value");
    invoker.setRequestTimeout(Duration.ofMillis(200));
    StepVerifier.create(service.never()).expectError(ServiceTimeoutException.class).verify(Duration.ofSeconds(5));
    assertThat(implementation.subscribed.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(implementation.cancelled.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(service.value()).isEqualTo("value");
  }

  @Test
  void blockingCallHasDeadline() {
    invoker.setRequestTimeout(Duration.ofMillis(200));
    assertThatThrownBy(service::slow).isInstanceOf(ServiceTimeoutException.class);
  }

  @Test
  void cancellationReachesServer() throws Exception {
    var subscription = service.never().subscribe();
    try {
      assertThat(implementation.subscribed.await(5, TimeUnit.SECONDS)).isTrue();
    }
    finally {
      subscription.dispose();
    }
    assertThat(implementation.cancelled.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(service.value()).isEqualTo("value");
  }

  @Test
  void futureValuesAndEmptyResults() throws Exception {
    assertThat(service.futureValue().join(Duration.ofSeconds(5))).isEqualTo("future");
    assertThat(service.futureNull().join(Duration.ofSeconds(5))).isNull();
    assertThat(service.futureVoid().join(Duration.ofSeconds(5))).isNull();
  }

  @Test
  void futureReturnsBeforeServerCompletion() throws Exception {
    Future<String> result = service.pendingFuture();
    assertThat(implementation.subscribed.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(result.isDone()).isFalse();
    implementation.pending.setSuccess("later");
    assertThat(result.join(Duration.ofSeconds(5))).isEqualTo("later");
  }

  @Test
  void futureFailuresDoNotBreakConnection() {
    assertThatThrownBy(() -> service.futureFailure().join(Duration.ofSeconds(5)))
            .hasMessageContaining("future failure");
    assertThatThrownBy(() -> service.futureThrow().join(Duration.ofSeconds(5)))
            .hasMessageContaining("sync future failure");
    assertThat(service.value()).isEqualTo("value");
  }

  @Test
  void futureTimeoutCancelsServerFuture() throws Exception {
    assertThat(service.value()).isEqualTo("value");
    invoker.setRequestTimeout(Duration.ofMillis(200));
    Future<String> result = service.pendingFuture();
    assertThatThrownBy(() -> result.join(Duration.ofSeconds(5))).isInstanceOf(ServiceTimeoutException.class);
    assertThat(implementation.cancelled.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(implementation.pending.isCancelled()).isTrue();
    assertThat(service.value()).isEqualTo("value");
  }

  @Test
  void futureCancellationReachesServer() throws Exception {
    Future<String> result = service.pendingFuture();
    try {
      assertThat(implementation.subscribed.await(5, TimeUnit.SECONDS)).isTrue();
    }
    finally {
      result.cancel();
    }
    assertThat(result.isCancelled()).isTrue();
    assertThat(implementation.cancelled.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(implementation.pending.isCancelled()).isTrue();
    assertThat(service.value()).isEqualTo("value");
  }

  @Test
  void customRegisteredTypeSupportsValueEmptyAndError() {
    StepVerifier.create(service.customValue().publisher()).expectNext("custom").verifyComplete();
    StepVerifier.create(service.customEmpty().publisher()).verifyComplete();
    StepVerifier.create(service.customFailure().publisher())
            .expectErrorMatches(error -> error.getMessage().contains("custom failure")).verify();
  }

  @Test
  void customRegisteredTypePropagatesCancellation() throws Exception {
    var subscription = service.customNever().publisher().subscribe();
    try {
      assertThat(implementation.subscribed.await(5, TimeUnit.SECONDS)).isTrue();
    }
    finally {
      subscription.dispose();
    }
    assertThat(implementation.cancelled.await(5, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  void customRegisteredTypePropagatesTimeout() throws Exception {
    assertThat(service.value()).isEqualTo("value");
    invoker.setRequestTimeout(Duration.ofMillis(200));
    StepVerifier.create(service.customNever().publisher()).expectError(ServiceTimeoutException.class)
            .verify(Duration.ofSeconds(5));
    assertThat(implementation.cancelled.await(5, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  void completionStageWorksThroughDefaultRegistry() throws Exception {
    assertThat(service.completable().get(5, TimeUnit.SECONDS)).isEqualTo("stage");
  }

  public record AsyncValue<T>(Mono<T> publisher) { }

  public interface TestService {
    String value();

    int number();

    String nullValue();

    void nothing();

    Mono<String> mono();

    Mono<String> empty();

    Mono<Void> monoVoid();

    String failure();

    Mono<String> monoFailure();

    Mono<String> monoThrow();

    Mono<String> never();

    String slow();

    Future<String> futureValue();

    Future<String> futureNull();

    Future<Void> futureVoid();

    Future<String> futureFailure();

    Future<String> futureThrow();

    Future<String> pendingFuture();

    AsyncValue<String> customValue();

    AsyncValue<String> customEmpty();

    AsyncValue<String> customFailure();

    AsyncValue<String> customNever();

    CompletableFuture<String> completable();
  }

  public static class TestServiceImpl implements TestService {
    final AtomicInteger calls = new AtomicInteger();
    final CountDownLatch subscribed = new CountDownLatch(1);
    final CountDownLatch cancelled = new CountDownLatch(1);
    final Promise<String> pending = Future.forPromise();

    public String value() {
      return "value";
    }

    public AsyncValue<String> customValue() {
      return new AsyncValue<>(Mono.just("custom"));
    }

    public AsyncValue<String> customEmpty() {
      return new AsyncValue<>(Mono.empty());
    }

    public AsyncValue<String> customFailure() {
      return new AsyncValue<>(Mono.error(new IllegalStateException("custom failure")));
    }

    public AsyncValue<String> customNever() {
      return new AsyncValue<>(never());
    }

    public CompletableFuture<String> completable() {
      return CompletableFuture.completedFuture("stage");
    }

    public Future<String> futureValue() {
      return Future.ok("future");
    }

    public Future<String> futureNull() {
      return Future.ok(null);
    }

    public Future<Void> futureVoid() {
      return Future.ok();
    }

    public Future<String> futureFailure() {
      return Future.failed(new IllegalArgumentException("future failure"));
    }

    public Future<String> futureThrow() {
      throw new IllegalStateException("sync future failure");
    }

    public Future<String> pendingFuture() {
      pending.onCompleted(completed -> {
        if (completed.isCancelled()) {
          cancelled.countDown();
        }
      });
      subscribed.countDown();
      return pending;
    }

    public int number() {
      return 42;
    }

    public String nullValue() {
      return null;
    }

    public void nothing() {
      calls.incrementAndGet();
    }

    public Mono<String> mono() {
      calls.incrementAndGet();
      return Mono.just("mono");
    }

    public Mono<String> empty() {
      return Mono.empty();
    }

    public Mono<Void> monoVoid() {
      return Mono.empty();
    }

    public String failure() {
      throw new IllegalStateException("sync failure");
    }

    public Mono<String> monoFailure() {
      return Mono.error(new IllegalArgumentException("async failure"));
    }

    public Mono<String> monoThrow() {
      throw new IllegalStateException("sync mono failure");
    }

    public Mono<String> never() {
      return Mono.<String>never().doOnSubscribe(s -> subscribed.countDown()).doOnCancel(cancelled::countDown);
    }

    public String slow() {
      try {
        Thread.sleep(5000);
      }
      catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
      return "slow";
    }
  }
}
