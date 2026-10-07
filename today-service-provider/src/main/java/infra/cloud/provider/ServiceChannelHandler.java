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

package infra.cloud.provider;

import org.reactivestreams.Publisher;

import infra.cloud.serialize.MessagePackReader;
import infra.cloud.service.DefaultMetadataCodec;
import infra.cloud.service.InvocationResult;
import infra.cloud.service.InvocationResults;
import infra.cloud.service.Metadata;
import infra.cloud.service.MetadataCodec;
import infra.cloud.service.SingleInvocationResult;
import infra.cloud.service.StreamElementParameter;
import infra.cloud.service.StreamingInvocationResult;
import infra.core.MethodParameter;
import infra.remoting.Channel;
import infra.remoting.Payload;
import infra.util.Assert;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/21 22:58
 */
public class ServiceChannelHandler implements Channel {

  private final RequestDeserializer requestDeserializer;

  private final ResponseSerializer responseSerializer;

  private final ServiceRequestExecutor requestExecutor;

  private final MetadataCodec metadataCodec;

  public ServiceChannelHandler(RequestDeserializer requestDeserializer, ResponseSerializer responseSerializer) {
    this(requestDeserializer, responseSerializer, new DefaultServiceRequestExecutor());
  }

  public ServiceChannelHandler(RequestDeserializer requestDeserializer, ResponseSerializer responseSerializer,
          ServiceRequestExecutor requestExecutor) {
    this(requestDeserializer, responseSerializer, requestExecutor, new DefaultMetadataCodec());
  }

  public ServiceChannelHandler(RequestDeserializer requestDeserializer, ResponseSerializer responseSerializer,
          ServiceRequestExecutor requestExecutor, MetadataCodec metadataCodec) {
    Assert.notNull(metadataCodec, "metadataCodec is required");
    this.requestDeserializer = requestDeserializer;
    this.responseSerializer = responseSerializer;
    this.requestExecutor = requestExecutor;
    this.metadataCodec = metadataCodec;
  }

  @Override
  public Mono<Payload> requestResponse(Payload payload) {
    final RemoteRequest request;
    try {
      Metadata metadata = payload.hasMetadata() ? metadataCodec.decode(payload.metadata()) : Metadata.empty();
      RemoteRequest decoded = requestDeserializer.deserialize(new MessagePackReader(payload.data()));
      request = metadata.isEmpty() ? decoded : new RemoteRequest(decoded.getMethod(), decoded.getArguments(),
              decoded.getServiceObject(), metadata);
    }
    catch (Throwable e) {
      return Mono.error(e);
    }
    finally {
      payload.release();
    }
    final InvocationResult result;
    try {
      result = requestExecutor.execute(request);
      if (!(result instanceof SingleInvocationResult)) {
        result.cancel();
        throw new IllegalStateException("request-response requires a single-value invocation result");
      }
    }
    catch (Throwable error) {
      Exceptions.throwIfFatal(error);
      return responseSerializer.serialize(request, error);
    }
    return Mono.from(InvocationResults.publisher(result))
            .flatMap(value -> responseSerializer.serialize(request, value))
            .switchIfEmpty(Mono.defer(() -> responseSerializer.serialize(request, (Object) null)))
            .onErrorResume(error -> responseSerializer.serialize(request, error))
            .doOnDiscard(Payload.class, Payload::release);
  }

  @Override
  public Flux<Payload> requestStream(Payload payload) {
    final RemoteRequest request;
    try {
      request = decodeRequest(payload);
    }
    catch (Throwable error) {
      Exceptions.throwIfFatal(error);
      return Flux.error(error);
    }
    return streamResponse(request);
  }

  @Override
  public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
    return Flux.from(payloads).switchOnFirst((signal, input) -> {
      if (!signal.hasValue()) {
        return signal.hasError() ? Flux.error(signal.getThrowable())
                : Flux.error(new IllegalArgumentException("Missing channel method header"));
      }
      Payload header = signal.get();
      final InvocableMethod method;
      final Metadata metadata;
      try {
        metadata = header.hasMetadata() ? metadataCodec.decode(header.metadata()) : Metadata.empty();
        method = requestDeserializer.deserializeMethod(new MessagePackReader(header.data()));
        Assert.isTrue(method.getParameters().length == 1
                        && method.getParameters()[0].getParameterType() == Flux.class,
                "Channel method requires one Flux argument");
      }
      catch (Throwable error) {
        Exceptions.throwIfFatal(error);
        return Flux.error(error);
      }
      finally {
        header.release();
      }

      MethodParameter parameter = StreamElementParameter.of(method.getParameters()[0]);
      Flux<Object> values = input.skip(1).map(payload -> {
        try {
          Assert.isTrue(!payload.hasMetadata(), "Channel metadata is only allowed on the header");
          return requestDeserializer.deserializeElement(parameter, new MessagePackReader(payload.data()));
        }
        finally {
          payload.release();
        }
      });
      RemoteRequest request = new RemoteRequest(method, new Object[] { values }, method.getServiceObject(), metadata);
      return streamResponse(request);
    }, true).doOnDiscard(Payload.class, payload -> {
      if (payload.refCnt() > 0) {
        payload.release();
      }
    });
  }

  @Override
  public Mono<Void> fireAndForget(Payload payload) {
    final RemoteRequest request;
    try {
      request = decodeRequest(payload);
    }
    catch (Throwable error) {
      Exceptions.throwIfFatal(error);
      return Mono.error(error);
    }
    return Mono.defer(() -> {
      InvocationResult result = requestExecutor.execute(request);
      if (result instanceof StreamingInvocationResult) {
        result.cancel();
        return Mono.error(new IllegalStateException("fire-and-forget does not accept streaming output"));
      }
      return Flux.from(InvocationResults.publisher(result)).then();
    });
  }

  private RemoteRequest decodeRequest(Payload payload) {
    try {
      Metadata metadata = payload.hasMetadata() ? metadataCodec.decode(payload.metadata()) : Metadata.empty();
      RemoteRequest request = requestDeserializer.deserialize(new MessagePackReader(payload.data()));
      return metadata.isEmpty() ? request : new RemoteRequest(request.getMethod(), request.getArguments(),
              request.getServiceObject(), metadata);
    }
    finally {
      payload.release();
    }
  }

  private Flux<Payload> streamResponse(RemoteRequest request) {
    return Flux.defer(() -> {
      InvocationResult result = requestExecutor.execute(request);
      if (!(result instanceof StreamingInvocationResult stream)) {
        result.cancel();
        return Flux.error(new IllegalStateException("Streaming interaction requires streaming output"));
      }
      return Flux.from(stream.values())
              .concatMap(value -> responseSerializer.serialize(request, value), 1)
              .onErrorResume(error -> responseSerializer.serialize(request, error));
    }).doOnDiscard(Payload.class, Payload::release);
  }

  @Override
  public Mono<Void> metadataPush(Payload payload) {
    return Channel.super.metadataPush(payload);
  }

}
