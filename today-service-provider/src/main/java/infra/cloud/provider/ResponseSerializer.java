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

package infra.cloud.provider;

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.cloud.serialize.MessagePackOutput;
import infra.cloud.serialize.Output;
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
    Output output = new MessagePackOutput(buffer);

    output.writeNullable(result, (out, v) -> {
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
