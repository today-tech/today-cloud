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

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.cloud.serialize.MessagePackWriter;
import infra.cloud.serialize.Writable;
import infra.cloud.serialize.ReturnValueSerializer;
import infra.cloud.service.ServiceMethod;
import infra.remoting.Payload;
import infra.remoting.util.ByteBufPayload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Mono;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/31 16:21
 */
public class ResponseSerializer {

  private final List<ReturnValueSerializer<?>> returnValueSerializers;

  private final ByteBufAllocator allocator = ByteBufAllocator.DEFAULT;

  public ResponseSerializer(List<ReturnValueSerializer<?>> returnValueSerializers) {
    this.returnValueSerializers = returnValueSerializers;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public Mono<Payload> serialize(RemoteRequest request, @Nullable Object result) {
    ByteBuf buffer = allocator.ioBuffer();
    Writable writable = new MessagePackWriter(buffer);

    writable.writeNullable(result, (out, v) -> {
      InvocableMethod invocableMethod = request.getMethod();
      ReturnValueSerializer serializer = findSerializer(invocableMethod);
      serializer.serialize(invocableMethod, v, out);
    });

    Payload payload = ByteBufPayload.create(buffer);
    return Mono.just(payload);
  }

  public Mono<Payload> serialize(RemoteRequest request, Throwable throwable) {
    ByteBuf buffer = allocator.ioBuffer();

    Payload payload = ByteBufPayload.create(buffer);
    return Mono.just(payload);
  }

  @SuppressWarnings("rawtypes")
  private ReturnValueSerializer findSerializer(ServiceMethod method) {
    for (ReturnValueSerializer serializer : returnValueSerializers) {
      if (serializer.supportsReturnValue(method)) {
        return serializer;
      }
    }
    throw new IllegalStateException("ReturnValueSerialization for method %s not found".formatted(method));
  }

}
