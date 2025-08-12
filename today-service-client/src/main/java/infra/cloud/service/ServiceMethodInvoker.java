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
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import infra.core.ReactiveAdapterRegistry;
import infra.lang.Nullable;
import infra.remoting.Payload;
import infra.remoting.RemotingOperations;
import infra.util.concurrent.Future;
import infra.util.concurrent.FutureListener;
import infra.util.concurrent.Promise;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;

/**
 * Service Method Invoker
 *
 * @author TODAY 2021/7/4 01:58
 */
public class ServiceMethodInvoker implements ServiceInvoker {

  private final ClientInterceptor[] interceptors;

  private final RemotingOperationsProvider remotingOperationsProvider;

  private final ServiceInterfaceMetadata<ServiceInterfaceMethod> metadata;

  ServiceMethodInvoker(ServiceInterfaceMetadata<ServiceInterfaceMethod> metadata,
          ClientInterceptor[] interceptors, RemotingOperationsProvider remotingOperationsProvider) {
    this.metadata = metadata;
    this.interceptors = interceptors;
    this.remotingOperationsProvider = remotingOperationsProvider;
  }

  @Override
  public InvocationResult invoke(ServiceInterfaceMethod serviceMethod, Object[] args) throws Throwable {
    MethodServiceInvocation invocation = new MethodServiceInvocation0(serviceMethod, args, interceptors);

    return invocation.proceed();
  }

  class MethodServiceInvocation0 extends MethodServiceInvocation {

    public MethodServiceInvocation0(ServiceInterfaceMethod serviceMethod, Object[] args, ClientInterceptor[] interceptors) {
      super(serviceMethod, args, interceptors);
    }

    @Override
    protected InvocationResult invokeRemoting() {
      RemotingOperations operations = remotingOperationsProvider.getRemotingOperations(getServiceMethod());

      Publisher<Payload> publisher = switch (getType()) {
        case FIRE_AND_FORGET -> operations.fireAndForget(createMonoPayload()).cast(Payload.class);
        case REQUEST_RESPONSE -> operations.requestResponse(createMonoPayload());
        case RESPONSE_STREAMING -> operations.requestStream(createMonoPayload());
        case DUPLEX_STREAMING -> operations.requestChannel(createChannelPayload());
      };

      return new InvocationResult0(getType(), publisher);
    }

    private Mono<Payload> createMonoPayload() {

      return null;
    }

    @SuppressWarnings("unchecked")
    private Publisher<Payload> createChannelPayload() {
      Flux<Object> flux = (Flux<Object>) getArguments()[0];

      return null;
    }

  }

  private Object deserialize(Payload payload) {
    return null;
  }

  class InvocationResult0 extends AbstractInvocationResult implements Subscriber<Payload>, FutureListener<Future<Object>> {

    @Nullable
    private Throwable throwable;

    private final InvocationType invocationType;

    private final Publisher<Payload> publisher;

    private final Promise<Object> resultPromise = Future.forPromise();

    @Nullable
    private Subscription payloadSubscription;

    public InvocationResult0(InvocationType invocationType, Publisher<Payload> publisher) {
      this.invocationType = invocationType;
      this.publisher = publisher;
      publisher.subscribe(this);
      resultPromise.onCompleted(this);
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
      ReactiveAdapterRegistry sharedInstance = ReactiveAdapterRegistry.getSharedInstance();
      return switch (getType()) {
        case FIRE_AND_FORGET -> Mono.from(publisher);
        case REQUEST_RESPONSE -> Mono.from(publisher).map(ServiceMethodInvoker.this::deserialize);
        case RESPONSE_STREAMING, DUPLEX_STREAMING -> Flux.from(publisher).map(ServiceMethodInvoker.this::deserialize);
      };
    }

    @Override
    public void onSubscribe(Subscription s) {
      if (Operators.validate(payloadSubscription, s)) {
        this.payloadSubscription = s;
        s.request(Long.MAX_VALUE);
      }
    }

    @Override
    public void onNext(Payload payload) {

    }

    @Override
    public void onError(Throwable t) {

    }

    @Override
    public void onComplete() {

    }

    @Override
    public void operationComplete(Future<Object> completed) {
      if (completed.isCancelled()) {
        if (payloadSubscription != null) {
          payloadSubscription.cancel();
        }
      }
    }
  }

}
