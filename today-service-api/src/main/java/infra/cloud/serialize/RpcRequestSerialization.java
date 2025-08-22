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

import java.util.List;

import infra.cloud.RpcRequest;
import infra.cloud.service.ServiceMethod;
import infra.core.MethodParameter;
import io.netty.buffer.ByteBuf;

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
  public void serialize(RpcRequest request, ByteBuf payload) {
    Output output = new MessagePackOutput(payload);
    request.writeTo(output);

    ServiceMethod method = request.getMethod();

    int idx = 0;
    Object[] arguments = request.getArguments();

    beforeSerializeArguments(output, arguments);
    for (MethodParameter parameter : method.getParameters()) {
      var serialization = findArgumentSerialization(parameter);
      serialization.serialize(parameter, arguments[idx++], output);
    }
    afterSerializeArguments(output, arguments);
  }

  private RpcArgumentSerialization findArgumentSerialization(MethodParameter parameter) {
    for (var argumentSerialization : argumentSerializations) {
      if (argumentSerialization.supportsArgument(parameter)) {
        return argumentSerialization;
      }
    }
    throw new IllegalStateException("RpcArgumentSerialization for parameter %s not found".formatted(parameter));
  }

  protected void afterSerializeArguments(Output output, Object[] arguments) {

  }

  protected void beforeSerializeArguments(Output output, Object[] arguments) {

  }

}
