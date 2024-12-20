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
import infra.cloud.core.serialize.DeserializeFailedException;
import infra.cloud.protocol.ByteBufInput;
import infra.core.MethodParameter;
import infra.lang.Nullable;
import io.netty.buffer.ByteBuf;
import io.protostuff.Output;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/12/20 15:59
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class RpcRequestSerialization {

  private final List<RpcArgumentSerialization> argumentSerializations;

  public RpcRequestSerialization(List<RpcArgumentSerialization> argumentSerializations) {
    this.argumentSerializations = argumentSerializations;
  }

  @SuppressWarnings("unchecked")
  public void serialize(RpcRequest request, ByteBuf payload, Output output) throws IOException {
    output.writeString(1, request.getMethodName(), true);
    output.writeString(2, request.getServiceName(), true);

    RpcMethod rpcMethod = request.getRpcMethod();

    int idx = 0;
    Object[] arguments = request.getArguments();
    beforeSerializeArguments(output, arguments);
    for (MethodParameter parameter : rpcMethod.getParameters()) {
      var serialization = findArgumentSerialization(parameter);
      if (serialization == null) {
        throw new IllegalStateException("RpcArgumentSerialization for parameter %s not found".formatted(parameter));
      }
      serialization.serialize(parameter, arguments[idx++], payload, output);
    }
    afterSerializeArguments(output, arguments);
  }

  @Nullable
  private RpcArgumentSerialization findArgumentSerialization(MethodParameter parameter) {
    for (var argumentSerialization : argumentSerializations) {
      if (argumentSerialization.supportsArgument(parameter)) {
        return argumentSerialization;
      }
    }
    return null;
  }

  protected void afterSerializeArguments(Output output, Object[] arguments) {

  }

  protected void beforeSerializeArguments(Output output, Object[] arguments) {

  }

  public RpcRequest deserialize(ByteBuf body) throws DeserializeFailedException {
    RpcRequest rpcRequest = new RpcRequest();
    ByteBufInput input = new ByteBufInput(body);

//    body.readInt();
//    body.readCharSequence();

    return rpcRequest;
  }

}
