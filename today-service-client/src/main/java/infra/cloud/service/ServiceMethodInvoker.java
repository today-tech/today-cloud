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

import infra.cloud.RpcRequest;
import infra.cloud.serialize.RpcRequestSerialization;
import infra.lang.Nullable;
import infra.remoting.Payload;
import infra.remoting.RemotingOperations;
import infra.remoting.util.ByteBufPayload;
import infra.util.concurrent.Future;
import infra.util.concurrent.FutureListener;
import infra.util.concurrent.Promise;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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

  private RpcRequestSerialization requestSerialization;

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
      return switch (getType()) {
        case FIRE_AND_FORGET -> new FireAndForgetResult(operations.fireAndForget(createMonoPayload()));
        case REQUEST_RESPONSE -> new RequestResponseResult(operations.requestResponse(createMonoPayload()));
        case RESPONSE_STREAMING -> new ResponseStreamingResult(operations.requestStream(createMonoPayload()));
        case DUPLEX_STREAMING -> new DuplexStreamingResult(operations.requestChannel(createChannelPayload()));
      };
    }

    private Mono<Payload> createMonoPayload() {
      return Mono.defer(() -> {
        RpcRequest request = new RpcRequest();
        request.setServiceClass(serviceMethod.getServiceInterface().getName());
        request.setMethodName(serviceMethod.getMethod().getName());

        ByteBuf buffer = Unpooled.buffer();
        Payload payload = ByteBufPayload.create(buffer);
        requestSerialization.serialize(request, buffer);
        return Mono.just(payload);
      });
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

  class InvocationResult0 extends AbstractInvocationResult implements Publisher<Object>,
          Subscriber<Payload>, FutureListener<Future<Object>>, Subscription {

    @Nullable
    private Throwable throwable;

    private final InvocationType invocationType;

    private final Publisher<Payload> payloadPublisher;

    @Nullable
    private Promise<Object> resultPromise;

    @Nullable
    private Subscription payloadSubscription;

    private Subscriber<? super Object> downstream;

    public InvocationResult0(InvocationType invocationType, Publisher<Payload> publisher) {
      this.invocationType = invocationType;
      this.payloadPublisher = publisher;
    }

    @Nullable
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
      if (resultPromise == null) {
        resultPromise = Future.forPromise();
      }
      return resultPromise;
    }

    @Override
    public Publisher<Object> publisher() {
      return this;
    }

    @Override
    public void subscribe(Subscriber<? super Object> downstream) {
      this.downstream = downstream;
      downstream.onSubscribe(this);
      payloadPublisher.subscribe(this);
      if (resultPromise != null) {
        resultPromise.onCompleted(this);
      }
    }

    @Override
    public void request(long n) {
      if (Operators.validate(n) && payloadSubscription != null) {
        payloadSubscription.request(n);
      }
    }

    @Override
    public void cancel() {
      if (payloadSubscription != null) {
        payloadSubscription.cancel();
        payloadSubscription = null;
      }

      if (resultPromise != null) {
        resultPromise.cancel();
      }
    }

    @Override
    public void onSubscribe(Subscription s) {
      if (Operators.validate(payloadSubscription, s)) {
        this.payloadSubscription = s;
      }
    }

    @Override
    public void onNext(Payload payload) {
      Object result = deserialize(payload);
      downstream.onNext(result);
      if (resultPromise != null) {
        resultPromise.trySuccess(result);
      }
    }

    @Override
    public void onError(Throwable t) {
      downstream.onError(t);
    }

    @Override
    public void onComplete() {
      downstream.onComplete();
      if (resultPromise != null && !resultPromise.isDone()) {
        resultPromise.trySuccess(null);
      }
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
