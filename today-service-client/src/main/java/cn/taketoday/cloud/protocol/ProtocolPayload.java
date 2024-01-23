/*
 * Copyright 2021 - 2023 the original author or authors.
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

package cn.taketoday.cloud.protocol;

import java.util.Map;

import cn.taketoday.lang.Nullable;
import io.netty.buffer.ByteBuf;

/**
 * Protocol
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/12/22 22:29
 */
public class ProtocolPayload {
  public static final int HEADER_LENGTH = 4 + 1 + 4;

  public final PayloadHeader header;

  @Nullable
  public final byte[] body;

  ProtocolPayload(PayloadHeader header, @Nullable byte[] body) {
    this.header = header;
    this.body = body;
  }

  public int getRequestId() {
    return header.requestId;
  }

  public ProtocolVersion getVersion() {
    return header.version;
  }

  @Nullable
  public Map<String, String> getMetadata() {
    return header.metadata;
  }

  public static ProtocolPayload decode(ByteBuf payload) {
    byte[] reserve = new byte[4];
    payload.readBytes(reserve);
    ProtocolVersion version = ProtocolVersion.valueOf(payload.readByte());
    int requestId = payload.readInt();
    PayloadHeader payloadHeader = new PayloadHeader(reserve, version, requestId);

    int bodyLength = payload.readableBytes();
    if (bodyLength != 0) {
      byte[] body = new byte[bodyLength];
      payload.readBytes(body);
      return new ProtocolPayload(payloadHeader, body);
    }
    return new ProtocolPayload(payloadHeader, null);
  }

}
