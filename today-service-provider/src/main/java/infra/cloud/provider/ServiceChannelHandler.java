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
import infra.remoting.Channel;
import infra.remoting.Payload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/21 22:58
 */
public class ServiceChannelHandler implements Channel {

  private final LocalServiceHolder localServiceHolder;

  private final RequestDeserializer requestDeserializer;

  private final ResponseSerializer responseSerializer;

  public ServiceChannelHandler(LocalServiceHolder localServiceHolder,
          RequestDeserializer requestDeserializer, ResponseSerializer responseSerializer) {
    this.localServiceHolder = localServiceHolder;
    this.requestDeserializer = requestDeserializer;
    this.responseSerializer = responseSerializer;
  }

  @Override
  public Mono<Payload> requestResponse(Payload payload) {
    RemoteRequest request = requestDeserializer.deserialize(new MessagePackReader(payload.data()));
    try {
      Object result = request.invoke();
      return responseSerializer.serialize(request, result);
    }
    catch (Throwable e) {
      return responseSerializer.serialize(request, e);
    }
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
