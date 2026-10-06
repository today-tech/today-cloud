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

  public ServiceChannelHandler(LocalServiceHolder localServiceHolder,
          RequestDeserializer requestDeserializer, ResponseSerializer responseSerializer) {
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
    return Channel.super.requestStream(payload);
  }

  @Override
  public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
    return Channel.super.requestChannel(payloads);
  }

  @Override
  public Mono<Void> fireAndForget(Payload payload) {
    return Channel.super.fireAndForget(payload);
  }

  @Override
  public Mono<Void> metadataPush(Payload payload) {
    return Channel.super.metadataPush(payload);
  }

}
