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

import infra.cloud.RpcResponse;
import infra.cloud.core.serialize.DeserializeFailedException;
import infra.cloud.protocol.ByteBufInput;
import io.netty.buffer.ByteBuf;
import io.protostuff.Input;
import io.protostuff.Output;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/12/20 21:22
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class RpcResponseSerialization {

  private final ReturnValueSerialization returnValueSerialization;

  public void serialize(RpcResponse response, ByteBuf payload, Output output) throws IOException {
    Throwable exception = response.getException();
    if (exception != null) {
      // has error
      payload.writeBoolean(true);
      output.writeString(1, exception.getClass().getName(), true);

    }
    else {
      payload.writeBoolean(false);
      Object result = response.getResult();
//      returnValueSerialization.serialize(result);
    }
  }

  public RpcResponse deserialize(ByteBuf body) throws DeserializeFailedException {
    RpcResponse response = new RpcResponse();
    boolean hasError = body.readBoolean();
    if (hasError) {


    }
    else {
      Input input = new ByteBufInput(body);
      Object result = returnValueSerialization.deserialize(, body, input);
      response.setResult(result);
    }
    return response;
  }

}
