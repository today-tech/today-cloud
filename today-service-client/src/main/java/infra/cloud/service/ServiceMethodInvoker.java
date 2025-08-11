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

import infra.lang.Nullable;
import infra.remoting.Payload;
import infra.remoting.RemotingOperations;
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
    protected InvocationResult invokeRemoting(Object[] args) {
      RemotingOperations operations = remotingOperationsProvider.getRemotingOperations(getServiceMethod());

      Publisher<Payload> publisher = switch (getType()) {
        case FIRE_AND_FORGET -> operations.fireAndForget(createMonoPayload(args)).cast(Payload.class);
        case REQUEST_RESPONSE -> operations.requestResponse(createMonoPayload(args));
        case RESPONSE_STREAMING -> operations.requestStream(createMonoPayload(args));
        case DUPLEX_STREAMING -> operations.requestChannel(createChannelPayload(args));
      };

      return new InvocationResult0(null, getType(), publisher);
    }

    private Mono<Payload> createMonoPayload(Object[] args) {

      return null;
    }

    @SuppressWarnings("unchecked")
    private Publisher<Payload> createChannelPayload(Object[] args) {
      Flux<Object> flux = (Flux<Object>) args[0];

      return null;
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
