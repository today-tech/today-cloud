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

import java.util.ArrayList;
import java.util.List;

import infra.cloud.serialize.MessagePackReader;
import infra.remoting.Channel;
import infra.remoting.Payload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/21 22:58
 */
public class ServiceChannelHandler implements Channel {

  private final LocalServiceHolder localServiceHolder;

  private final RequestDeserializer requestDeserializer;

  private final ResponseSerializer responseSerializer;

  private final List<ReturnValueHandler> returnValueHandlers;

  public ServiceChannelHandler(LocalServiceHolder localServiceHolder,
          RequestDeserializer requestDeserializer, ResponseSerializer responseSerializer) {
    this(localServiceHolder, requestDeserializer, responseSerializer, List.of());
  }

  public ServiceChannelHandler(LocalServiceHolder localServiceHolder,
          RequestDeserializer requestDeserializer, ResponseSerializer responseSerializer,
          List<ReturnValueHandler> returnValueHandlers) {
    this.localServiceHolder = localServiceHolder;
    this.requestDeserializer = requestDeserializer;
    this.responseSerializer = responseSerializer;
    var handlers = new ArrayList<>(returnValueHandlers);
    handlers.add(new ReactiveReturnValueHandler());
    handlers.add(new SimpleReturnValueHandler());
    this.returnValueHandlers = List.copyOf(handlers);
  }

  @Override
  public Mono<Payload> requestResponse(Payload payload) {
    final RemoteRequest request;
    try {
      request = requestDeserializer.deserialize(new MessagePackReader(payload.data()));
    }
    catch (Throwable e) {
      return Mono.error(e);
    }
    finally {
      payload.release();
    }
    return Mono.defer(() -> {
              try {
                Object result = request.invoke();
                for (ReturnValueHandler handler : returnValueHandlers) {
                  if (handler.supportsReturnValue(request.getMethod())) {
                    return handler.handleReturnValue(request, result)
                            .flatMap(value -> responseSerializer.serialize(request, value))
                            .switchIfEmpty(Mono.defer(() -> responseSerializer.serialize(request, (Object) null)))
                            .onErrorResume(error -> responseSerializer.serialize(request, error));
                  }
                }
                return Mono.error(new IllegalStateException("No ReturnValueHandler for " + request.getMethod()));
              }
              catch (Throwable e) {
                return responseSerializer.serialize(request, e);
              }
            }).subscribeOn(Schedulers.boundedElastic())
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
