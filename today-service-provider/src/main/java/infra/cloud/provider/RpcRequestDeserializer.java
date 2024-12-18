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

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import infra.cloud.RpcRequest;
import infra.cloud.core.serialize.ProtostuffUtils;
import io.netty.buffer.ByteBuf;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/8 23:15
 */
public class RpcRequestDeserializer {

  public RpcRequest deserialize(ByteBuf body) throws IOException {
    RpcRequest request = ProtostuffUtils.deserialize(body.nioBuffer(), RpcRequest.class);
    int length = body.readInt();
    CharSequence charSequence = body.readCharSequence(length, StandardCharsets.UTF_8);

    // TODO

    return request;
  }
}
