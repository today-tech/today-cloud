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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

import infra.cloud.ServiceTimeoutException;
import infra.cloud.serialize.MessagePackWriter;
import infra.cloud.service.serialize.RequestSerializer;
import infra.cloud.service.serialize.ResponseDeserializer;
import infra.core.MethodParameter;
import infra.remoting.Payload;
import infra.remoting.RemotingOperations;
import infra.remoting.util.ByteBufPayload;
import infra.util.Assert;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

  private final InterceptorChain chain;

  private final RemotingOperationsProvider remotingOperationsProvider;

  private final ByteBufAllocator allocator;

  private final RequestSerializer requestSerializer;

  private final ResponseDeserializer responseDeserializer;

  private Duration requestTimeout = Duration.ofSeconds(30);

  private MetadataCodec metadataCodec = new DefaultMetadataCodec();

  public void setMetadataCodec(MetadataCodec metadataCodec) {
    Assert.notNull(metadataCodec, "metadataCodec is required");
    this.metadataCodec = metadataCodec;
  }

  /**
   * Set the deadline for a request-response subscription, including connection acquisition.
   */
  public void setRequestTimeout(Duration requestTimeout) {
    Assert.notNull(requestTimeout, "requestTimeout is required");
    Assert.isTrue(!requestTimeout.isZero() && !requestTimeout.isNegative(), "requestTimeout must be positive");
    this.requestTimeout = requestTimeout;
  }

  public ServiceMethodInvoker(List<ClientInterceptor> interceptors, RemotingOperationsProvider remotingOperationsProvider,
          ByteBufAllocator allocator, RequestSerializer requestSerializer, ResponseDeserializer responseDeserializer) {
    this.chain = new DefaultInterceptorChain(interceptors, new RemoteInvocation());
    this.remotingOperationsProvider = remotingOperationsProvider;
    this.allocator = allocator;
    this.requestSerializer = requestSerializer;
    this.responseDeserializer = responseDeserializer;
  }

  @Override
  public InvocationResult invoke(ServiceInterfaceMethod serviceMethod, Object[] args) throws Exception {
    return chain.proceed(new DefaultClientRequest(serviceMethod, args));
  }

  private final class RemoteInvocation implements InterceptorChain {

    @Override
    public InvocationResult proceed(ClientRequest request) {
      ServiceInterfaceMethod serviceMethod = request.getServiceMethod();
      RemotingOperations operations = remotingOperationsProvider.getRemotingOperations(serviceMethod);
      return switch (request.getType()) {
        case FIRE_AND_FORGET -> new FireAndForgetResult(operations.fireAndForget(createMonoPayload(request)));
        case REQUEST_RESPONSE -> new RequestResponseResult(serviceMethod, operations.requestResponse(createMonoPayload(request)).timeout(requestTimeout)
                .onErrorMap(TimeoutException.class, error -> new ServiceTimeoutException("Service request timed out: " + serviceMethod.getMethod(), error)), responseDeserializer);
        case RESPONSE_STREAMING -> new ResponseStreamingResult(serviceMethod, operations.requestStream(createMonoPayload(request)), responseDeserializer);
        case DUPLEX_STREAMING -> new DuplexStreamingResult(serviceMethod, operations.requestChannel(createChannelPayload(request)), responseDeserializer);
      };
    }

    private Mono<Payload> createMonoPayload(ClientRequest invocation) {
      ServiceInterfaceMethod serviceMethod = invocation.getServiceMethod();
      return Mono.defer(() -> {
        ByteBuf buffer = allocator.ioBuffer();
        ByteBuf metadataBuffer = null;
        try {
          requestSerializer.serialize(serviceMethod, invocation.getArguments(), new MessagePackWriter(buffer));
          Metadata metadata = invocation.getMetadata().snapshot();
          if (!metadata.isEmpty()) {
            metadataBuffer = allocator.ioBuffer();
            metadataCodec.encode(metadata, metadataBuffer);
          }
          return Mono.just(metadataBuffer == null
                  ? ByteBufPayload.create(buffer)
                  : ByteBufPayload.create(buffer, metadataBuffer));
        }
        catch (Throwable ex) {
          buffer.release();
          if (metadataBuffer != null) {
            metadataBuffer.release();
          }
          return Mono.error(ex);
        }
      });
    }

    @SuppressWarnings("unchecked")
    private Publisher<Payload> createChannelPayload(ClientRequest request) {
      return Flux.defer(() -> {
        ServiceInterfaceMethod method = request.getServiceMethod();
        Assert.isTrue(method.getParameters().length == 1, "Channel requires one stream argument");
        MethodParameter parameter = StreamElementParameter.of(method.getParameters()[0]);
        Flux<Object> input = Flux.from((Publisher<Object>) request.getArguments()[0]);
        Mono<Payload> header = Mono.fromSupplier(() -> encodeChannelPayload(parameter, request, null));
        return header.concatWith(input.map(value -> encodeChannelPayload(parameter, request, value)))
                .doOnDiscard(Payload.class, Payload::release);
      });
    }

    private Payload encodeChannelPayload(MethodParameter parameter, ClientRequest request, @Nullable Object value) {
      ByteBuf buffer = allocator.ioBuffer();
      ByteBuf metadataBuffer = null;
      try {
        if (value == null) {
          requestSerializer.serializeHeader(request.getServiceMethod(), new MessagePackWriter(buffer));
          Metadata metadata = request.getMetadata().snapshot();
          if (!metadata.isEmpty()) {
            metadataBuffer = allocator.ioBuffer();
            metadataCodec.encode(metadata, metadataBuffer);
          }
        }
        else {
          requestSerializer.serializeElement(parameter, value, new MessagePackWriter(buffer));
        }
        return metadataBuffer == null ? ByteBufPayload.create(buffer) : ByteBufPayload.create(buffer, metadataBuffer);
      }
      catch (Throwable error) {
        buffer.release();
        if (metadataBuffer != null) {
          metadataBuffer.release();
        }
        throw error;
      }
    }

  }

}
