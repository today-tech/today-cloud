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

package infra.cloud.serialize;

import java.io.IOException;
import java.util.List;

import infra.cloud.RpcMethod;
import infra.cloud.RpcRequest;
import infra.cloud.RpcResponse;
import io.netty.buffer.ByteBuf;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/12/20 21:22
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class RpcResponseSerialization {

  private final List<ReturnValueSerialization> serializations;

  private final ThrowableSerialization throwableSerialization;

  public RpcResponseSerialization(List<ReturnValueSerialization> serializations, ThrowableSerialization throwableSerialization) {
    this.serializations = serializations;
    this.throwableSerialization = throwableSerialization;
  }

  public void serialize(RpcResponse response, ByteBuf payload) throws IOException {
    Throwable exception = response.getException();
    if (exception != null) {
      // has error
      payload.writeBoolean(true);
      throwableSerialization.serialize(exception, payload);
    }
    else {
      RpcMethod rpcMethod = response.getRpcMethod();
      payload.writeBoolean(false);

      var serialization = findSerialization(rpcMethod);
      Object result = response.getResult();
      serialization.serialize(rpcMethod, result, payload, new DefaultByteBufOutput(payload));
    }
  }

  public RpcResponse deserialize(RpcRequest rpcRequest, ByteBuf body) throws SerializationException {
    RpcMethod rpcMethod = rpcRequest.getRpcMethod();
    RpcResponse response = new RpcResponse();
    response.setRpcMethod(rpcMethod);
    boolean hasError = body.readBoolean();
    if (hasError) {
      Throwable deserialize = throwableSerialization.deserialize(body);
      response.setException(deserialize);
    }
    else {
      var serialization = findSerialization(rpcMethod);
      Object result = serialization.deserialize(rpcMethod, body, new DefaultByteBufInput(body));
      response.setResult(result);
    }
    return response;
  }

  private ReturnValueSerialization findSerialization(RpcMethod rpcMethod) {
    for (ReturnValueSerialization serialization : serializations) {
      if (serialization.supportsArgument(rpcMethod)) {
        return serialization;
      }
    }
    throw new IllegalStateException("ReturnValueSerialization for method %s not found".formatted(rpcMethod));
  }

}
