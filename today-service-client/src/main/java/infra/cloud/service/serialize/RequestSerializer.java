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

import infra.cloud.RpcRequest;
import infra.cloud.serialize.MessagePackWriter;
import infra.cloud.serialize.Writable;
import infra.cloud.serialize.RpcArgumentSerialization;
import infra.cloud.service.ServiceMethod;
import infra.core.MethodParameter;
import io.netty.buffer.ByteBuf;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/12/20 15:59
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class RequestSerializer {

  private final List<RpcArgumentSerialization> argumentSerializations;

  public RequestSerializer(List<RpcArgumentSerialization> argumentSerializations) {
    this.argumentSerializations = argumentSerializations;
  }

  @SuppressWarnings("unchecked")
  public void serialize(RpcRequest request, ByteBuf payload) {
    Writable writable = new MessagePackWriter(payload);
    request.writeTo(writable);

    ServiceMethod method = request.getMethod();

    int idx = 0;
    Object[] arguments = request.getArguments();

    beforeSerializeArguments(writable, arguments);
    for (MethodParameter parameter : method.getParameters()) {
      var serialization = findArgumentSerialization(parameter);
      serialization.serialize(parameter, arguments[idx++], writable);
    }
    afterSerializeArguments(writable, arguments);
  }

  private RpcArgumentSerialization findArgumentSerialization(MethodParameter parameter) {
    for (var argumentSerialization : argumentSerializations) {
      if (argumentSerialization.supportsArgument(parameter)) {
        return argumentSerialization;
      }
    }
    throw new IllegalStateException("RpcArgumentSerialization for parameter %s not found".formatted(parameter));
  }

  protected void afterSerializeArguments(Writable writable, Object[] arguments) {

  }

  protected void beforeSerializeArguments(Writable writable, Object[] arguments) {

  }

}
