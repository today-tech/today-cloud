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
import java.util.Random;

import cn.taketoday.lang.Nullable;
import io.netty.buffer.ByteBuf;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/12/22 22:23
 */
public class PayloadHeader {
  static final Random random = new Random();

  public final byte[] reserve;

  public final ProtocolVersion version;

  public final int requestId;

  @Nullable
  public final Map<String, String> metadata;

  public PayloadHeader(byte[] reserve, ProtocolVersion version, int requestId) {
    this.reserve = reserve;
    this.version = version;
    this.requestId = requestId;
    this.metadata = null;
  }

  public void serialize(ByteBuf header) {
    serialize(header, version, requestId);
  }

  public static void serialize(ByteBuf header, int requestId) {
    serialize(header, ProtocolVersion.CURRENT, requestId);
  }

  public static void serialize(ByteBuf header, ProtocolVersion version, int requestId) {
    byte[] reserve = new byte[4];
    random.nextBytes(reserve);
    header.writeBytes(reserve);
    header.writeByte(version.asByte());
    header.writeInt(requestId);
  }

}
