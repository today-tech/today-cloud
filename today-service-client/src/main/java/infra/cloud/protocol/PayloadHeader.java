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

package infra.cloud.protocol;

import java.util.Map;
import java.util.Random;

import infra.lang.Nullable;
import io.netty.buffer.ByteBuf;

/**
 * Payload header
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/12/22 22:23
 */
public class PayloadHeader {
  // 0     4          5            9           11
  // reserve -> version -> requestId -> eventType -> metadata

  static final Random random = new Random();

  public final int reserve;

  public final ProtocolVersion version;

  public final int requestId;

  public final RemoteEventType eventType;

  @Nullable
  public final Map<String, String> metadata; // TODO metadata

  public PayloadHeader(int reserve, ProtocolVersion version, int requestId, RemoteEventType eventType) {
    this.reserve = reserve;
    this.version = version;
    this.requestId = requestId;
    this.eventType = eventType;
    this.metadata = null;
  }

  @Nullable
  public Map<String, String> getMetadata() {
    return metadata;
  }

  @Nullable
  public String getMetadata(String key) {
    return metadata != null ? metadata.get(key) : null;
  }

  public void serialize(ByteBuf header) {
    int reserve = random.nextInt();
    header.writeInt(reserve);
    header.writeByte(version.asByte());
    header.writeInt(requestId);
    header.writeShort(eventType.value);
  }

  public static void serialize(ByteBuf header, int requestId, RemoteEventType eventType) {
    int reserve = random.nextInt();
    header.writeInt(reserve);
    header.writeByte(ProtocolVersion.CURRENT.asByte());
    header.writeInt(requestId);
    header.writeShort(eventType.value);
  }

}
