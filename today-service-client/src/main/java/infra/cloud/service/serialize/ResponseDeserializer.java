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

package infra.cloud.service.serialize;

import java.util.List;

import infra.cloud.RpcResponse;
import infra.cloud.serialize.MessagePackInput;
import infra.cloud.serialize.ReturnValueDeserializer;
import infra.cloud.serialize.SerializationException;
import infra.cloud.serialize.ThrowableSerialization;
import infra.cloud.service.ServiceInterfaceMethod;
import infra.cloud.service.ServiceMethod;
import io.netty.buffer.ByteBuf;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/12/20 21:22
 */
@SuppressWarnings({ "rawtypes" })
public class ResponseDeserializer {

  private final List<ReturnValueDeserializer> serializations;

  private final ThrowableSerialization throwableSerialization;

  public ResponseDeserializer(List<ReturnValueDeserializer> serializations, ThrowableSerialization throwableSerialization) {
    this.serializations = serializations;
    this.throwableSerialization = throwableSerialization;
  }

  public RpcResponse deserialize(ServiceInterfaceMethod method, ByteBuf body) throws SerializationException {
    MessagePackInput input = new MessagePackInput(body);
    RpcResponse response = new RpcResponse();
    response.setMethod(method);

    boolean hasError = body.readBoolean();
    if (hasError) {
      Throwable deserialize = throwableSerialization.deserialize(body);
      response.setException(deserialize);
    }
    else {
      Object result = input.readNullable(in -> {
        var deserializer = findDeserializer(method);
        return deserializer.deserialize(method, input);
      });
      response.setResult(result);
    }
    return response;
  }

  private ReturnValueDeserializer findDeserializer(ServiceMethod method) {
    for (ReturnValueDeserializer deserializer : serializations) {
      if (deserializer.supportsReturnValue(method)) {
        return deserializer;
      }
    }
    throw new IllegalStateException("ReturnValueDeserializer for method %s not found".formatted(method));
  }

}
