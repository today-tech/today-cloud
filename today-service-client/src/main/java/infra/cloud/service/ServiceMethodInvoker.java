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

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.List;

import infra.cloud.serialize.MessagePackWriter;
import infra.cloud.service.serialize.RequestSerializer;
import infra.cloud.service.serialize.ResponseDeserializer;
import infra.remoting.Payload;
import infra.remoting.RemotingOperations;
import infra.remoting.util.ByteBufPayload;
import infra.util.concurrent.Future;
import infra.util.concurrent.FutureListener;
import infra.util.concurrent.Promise;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;

/**
 * Invokes service methods by handling different invocation types such as fire-and-forget,
 * request-response, response streaming, and duplex streaming. This class manages the
 * serialization of requests, deserialization of responses, and interacts with remoting
 * operations to execute remote procedure calls (RPC).
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2021/7/4 01:58
 */
public class ServiceMethodInvoker implements ServiceInvoker {

  private final ClientInterceptor[] interceptors;

  private final RemotingOperationsProvider remotingOperationsProvider;

  private final ByteBufAllocator allocator;

  private final RequestSerializer requestSerializer;

  private final ResponseDeserializer responseDeserializer;

  public ServiceMethodInvoker(List<ClientInterceptor> interceptors, RemotingOperationsProvider remotingOperationsProvider,
          ByteBufAllocator allocator, RequestSerializer requestSerializer, ResponseDeserializer responseDeserializer) {
    this.interceptors = interceptors.toArray(new ClientInterceptor[0]);
    this.remotingOperationsProvider = remotingOperationsProvider;
    this.allocator = allocator;
    this.requestSerializer = requestSerializer;
    this.responseDeserializer = responseDeserializer;
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
        case REQUEST_RESPONSE -> new RequestResponseResult(getServiceMethod(), operations.requestResponse(createMonoPayload()), responseDeserializer);
        case RESPONSE_STREAMING -> new ResponseStreamingResult(getServiceMethod(), operations.requestStream(createMonoPayload()), responseDeserializer);
        case DUPLEX_STREAMING -> new DuplexStreamingResult(operations.requestChannel(createChannelPayload()));
      };
    }

    private Mono<Payload> createMonoPayload() {
      return Mono.defer(() -> {
        ByteBuf buffer = allocator.ioBuffer();
        requestSerializer.serialize(serviceMethod, getArguments(), new MessagePackWriter(buffer));
        return Mono.just(ByteBufPayload.create(buffer));
      });
    }

    @SuppressWarnings("unchecked")
    private Publisher<Payload> createChannelPayload() {
      Flux<Object> flux = (Flux<Object>) getArguments()[0];

      return Flux.empty();
    }

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
