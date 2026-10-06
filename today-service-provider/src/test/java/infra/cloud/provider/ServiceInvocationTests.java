package infra.cloud.provider;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import infra.cloud.service.ServiceMetadata;
import infra.core.ReactiveAdapterRegistry;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests service execution without a transport, payload, or request codec.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2026/10/5
 */
class ServiceInvocationTests {

  private DefaultServiceMethodResolver resolver(ServiceObject service) {
    var holder = mock(LocalServiceHolder.class);
    when(holder.getServiceObject(Example.class.getName())).thenReturn(service);
    return resolver(holder);
  }

  private DefaultServiceMethodResolver resolver(LocalServiceHolder holder) {
    return new DefaultServiceMethodResolver(new DefaultServiceInterfaceMetadataProvider(
            type -> new ServiceMetadata("test", "1", List.of(type.getName()))), new ReactiveAdapterRegistry(), holder);
  }

  @Test
  void resolvesOverloadsAndCachesSignaturesByContents() {
    var service = new ServiceObject(Example.class, new ExampleImpl());
    var resolver = resolver(service);
    String[] types = { String.class.getName() };
    var method = resolver.resolve(Example.class.getName(), "echo", types);
    types[0] = "changed";
    assertThat(resolver.resolve(Example.class.getName(), "echo", new String[] { String.class.getName() })).isSameAs(method);
    assertThat(method.invoke(new Object[] { "hello" })).isEqualTo("hello");
    assertThat(resolver.resolve(Example.class.getName(), "echo", new String[] { "int" }).invoke(new Object[] { 42 })).isEqualTo(42);
    assertThatThrownBy(() -> resolver.resolve(Example.class.getName(), "implementationOnly", new String[0]))
            .hasMessageContaining("No exported method");
  }

  @Test
  void cachedMethodDoesNotCrossServiceInstances() {
    var first = new ExampleImpl();
    var second = new ExampleImpl();
    var holder = mock(LocalServiceHolder.class);
    when(holder.getServiceObject(Example.class.getName())).thenReturn(
            new ServiceObject(Example.class, first), new ServiceObject(Example.class, second));
    var resolver = resolver(holder);
    var firstMethod = resolver.resolve(Example.class.getName(), "count", new String[0]);
    var secondMethod = resolver.resolve(Example.class.getName(), "count", new String[0]);
    firstMethod.invoke(null);
    assertThat(first.calls).hasValue(1);
    assertThat(second.calls).hasValue(0);
    assertThat(secondMethod).isNotSameAs(firstMethod);
  }

  @Test
  void invocationIsLazyAndIndependentOfTransport() {
    var implementation = new ExampleImpl();
    var service = new ServiceObject(Example.class, implementation);
    var method = resolver(service).resolve(Example.class.getName(), "count", new String[0]);
    var request = new RemoteRequest(method, null, service);
    var executor = new DefaultServiceRequestExecutor(Schedulers.immediate());
    var result = executor.execute(request);
    assertThat(implementation.calls).hasValue(0);
    StepVerifier.create(result).expectNext(1).verifyComplete();
    StepVerifier.create(result).expectError(IllegalStateException.class).verify();
    assertThat(implementation.calls).hasValue(1);
  }

  @Test
  void emptyAndFailedResultsHaveUniformSemantics() {
    var service = new ServiceObject(Example.class, new ExampleImpl());
    var resolver = resolver(service);
    var executor = new DefaultServiceRequestExecutor(Schedulers.immediate());
    StepVerifier.create(executor.execute(new RemoteRequest(resolver.resolve(Example.class.getName(), "empty", new String[0]), null, service)))
            .verifyComplete();
    StepVerifier.create(executor.execute(new RemoteRequest(resolver.resolve(Example.class.getName(), "failure", new String[0]), null, service)))
            .expectErrorMatches(error -> error.getMessage().contains("failure")).verify();
  }

  @Test
  void customHandlerIsSelectedOnceAndBoundToCachedMethod() {
    var selections = new AtomicInteger();
    var adaptations = new AtomicInteger();
    var custom = new ReturnValueHandler() {
      public boolean supportsReturnValue(infra.cloud.service.ServiceMethod method) {
        selections.incrementAndGet();
        return method.getMethod().getName().equals("count");
      }

      public Mono<Object> handleReturnValue(RemoteRequest request, Object value) {
        adaptations.incrementAndGet();
        return Mono.just("adapted-" + value);
      }
    };
    var service = new ServiceObject(Example.class, new ExampleImpl());
    var holder = mock(LocalServiceHolder.class);
    when(holder.getServiceObject(Example.class.getName())).thenReturn(service);
    var resolver = new DefaultServiceMethodResolver(new DefaultServiceInterfaceMetadataProvider(
            type -> new ServiceMetadata("test", "1", List.of(type.getName()))), new ReactiveAdapterRegistry(), holder,
            new ReturnValueHandlerComposite(List.of(custom)));
    var method = resolver.resolve(Example.class.getName(), "count", new String[0]);
    assertThat(method.getReturnValueHandler()).isSameAs(custom);
    assertThat(resolver.resolve(Example.class.getName(), "count", new String[0])).isSameAs(method);
    var executor = new DefaultServiceRequestExecutor(Schedulers.immediate());
    var result = executor.execute(new RemoteRequest(method, null, service));
    StepVerifier.create(result).expectNext("adapted-1").verifyComplete();
    StepVerifier.create(result).expectError(IllegalStateException.class).verify();
    StepVerifier.create(executor.execute(new RemoteRequest(method, null, service)))
            .expectNext("adapted-2").verifyComplete();
    assertThat(selections).hasValue(1);
    assertThat(adaptations).hasValue(2);
  }

  public interface Example {
    String echo(String value);
    int echo(int value);
    int count();
    Mono<String> empty();
    String failure();
  }

  public static class ExampleImpl implements Example {
    final AtomicInteger calls = new AtomicInteger();
    public String echo(String value) { return value; }
    public int echo(int value) { return value; }
    public int count() { return calls.incrementAndGet(); }
    public Mono<String> empty() { return Mono.empty(); }
    public String failure() { throw new IllegalStateException("failure"); }
    public String implementationOnly() { return "private operation"; }
  }
}
