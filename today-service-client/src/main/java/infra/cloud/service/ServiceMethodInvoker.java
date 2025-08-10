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

package infra.cloud.service;

import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import infra.cloud.client.DiscoveryClient;
import infra.cloud.client.ServiceInstance;
import infra.lang.Nullable;
import infra.remoting.Payload;
import infra.remoting.core.RemotingClient;
import infra.remoting.lb.LoadBalanceTarget;
import infra.remoting.transport.netty.client.TcpClientTransport;
import infra.util.concurrent.Future;
import infra.util.concurrent.Promise;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Method Invoker
 *
 * @author TODAY 2021/7/4 01:58
 */
public class ServiceMethodInvoker implements ServiceInvoker {

  private final DiscoveryClient discoveryClient;

  private final ServiceInterfaceMetadata<ServiceInterfaceMethod> metadata;

  private final ClientInterceptor[] interceptors;

  private Duration discoveryPeriod = Duration.ofSeconds(10);

  ServiceMethodInvoker(ServiceInterfaceMetadata<ServiceInterfaceMethod> metadata,
          DiscoveryClient discoveryClient, ClientInterceptor[] interceptors) {
    this.metadata = metadata;
    this.discoveryClient = discoveryClient;
    this.interceptors = interceptors;
  }

  /**
   * service discovery reload period
   */
  public void setDiscoveryPeriod(Duration discoveryPeriod) {
    this.discoveryPeriod = discoveryPeriod;
  }

  @Override
  public InvocationResult invoke(ServiceInterfaceMethod serviceMethod, Object[] args) throws Throwable {
    MethodServiceInvocation invocation = new MethodServiceInvocation0(serviceMethod, args, interceptors);

    return invocation.proceed();
  }

  class MethodServiceInvocation0 extends MethodServiceInvocation
          implements Function<List<ServiceInstance>, List<LoadBalanceTarget>> {

    public MethodServiceInvocation0(ServiceInterfaceMethod serviceMethod, Object[] args, ClientInterceptor[] interceptors) {
      super(serviceMethod, args, interceptors);
    }

    @Override
    protected InvocationResult invokeRemoting(Object[] args) {
      RemotingClient client = RemotingClient.forLoadBalance(Flux.interval(discoveryPeriod)
                      .map(i -> discoveryClient.getInstances(getServiceMetadata().getName()))
                      .map(this))
              .roundRobinLoadBalanceStrategy()
              .build();

      Publisher<Payload> publisher = switch (getType()) {
        case FIRE_AND_FORGET -> client.fireAndForget(createMonoPayload(args)).cast(Payload.class);
        case REQUEST_RESPONSE -> client.requestResponse(createMonoPayload(args));
        case RESPONSE_STREAMING -> client.requestStream(createMonoPayload(args));
        case DUPLEX_STREAMING -> client.requestChannel(createChannelPayload(args));
      };

      return new InvocationResult0(null, getType(), publisher);
    }

    private Mono<Payload> createMonoPayload(Object[] args) {
      return null;
    }

    private Publisher<Payload> createChannelPayload(Object[] args) {

      return null;
    }

    @Override
    public List<LoadBalanceTarget> apply(List<ServiceInstance> serviceInstances) {
      var targets = new ArrayList<LoadBalanceTarget>(serviceInstances.size());
      for (ServiceInstance instance : serviceInstances) {
        targets.add(LoadBalanceTarget.of(instance.getInstanceId(),
                TcpClientTransport.create(instance.getHost(), instance.getPort())));
      }
      return targets;
    }
  }

  private Object deserialize(Payload payload) {
    return null;
  }

  class InvocationResult0 extends AbstractInvocationResult {

    @Nullable
    private final Throwable throwable;

    private final InvocationType invocationType;

    private final Publisher<Payload> publisher;

    private final Promise<Object> resultPromise = Future.forPromise();

    public InvocationResult0(@Nullable Throwable throwable, InvocationType invocationType, Publisher<Payload> publisher) {
      this.throwable = throwable;
      this.invocationType = invocationType;
      this.publisher = publisher;
    }

    @Override
    public Object getValue() {
      return future().join();
    }

    @Override
    public boolean isFailed() {
      return throwable != null;
    }

    @Nullable
    @Override
    public Throwable getException() {
      return throwable;
    }

    @Override
    public InvocationType getType() {
      return invocationType;
    }

    @Override
    public Future<Object> future() {
      // TODO
      return resultPromise;
    }

    @Override
    public Publisher<Object> publisher() {
      return switch (getType()) {
        case FIRE_AND_FORGET -> Mono.from(publisher);
        case REQUEST_RESPONSE -> Mono.from(publisher).map(ServiceMethodInvoker.this::deserialize);
        case RESPONSE_STREAMING, DUPLEX_STREAMING -> Flux.from(publisher).map(ServiceMethodInvoker.this::deserialize);
      };
    }

  }

}
