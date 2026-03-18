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

package infra.cloud.service.serialize;

import java.util.List;

import infra.cloud.RpcResponse;
import infra.cloud.serialize.MessagePackReader;
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
    MessagePackReader input = new MessagePackReader(body);
    RpcResponse response = new RpcResponse();
    response.setMethod(method);

    Object result = input.readNullable(in -> {
      var deserializer = findDeserializer(method);
      return deserializer.deserialize(method, input);
    });
    response.setResult(result);
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
