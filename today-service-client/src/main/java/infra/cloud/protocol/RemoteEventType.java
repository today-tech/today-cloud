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

/**
 * Remote event type
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/3/8 22:30
 */
public enum RemoteEventType {

  RPC_REQUEST(1),

  RPC_RESPONSE(2),

  NOTIFY(3),

  PING(4),

  PONG(5)

  ;

  public final int value;

  RemoteEventType(int value) {
    this.value = value;
  }

  public static RemoteEventType forValue(short eventType) {
    return switch (eventType) {
      case 1 -> RPC_REQUEST;
      case 2 -> RPC_RESPONSE;
      case 3 -> NOTIFY;
      case 4 -> PING;
      case 5 -> PONG;
      default -> throw new ProtocolParsingException("Event type: [%s] not supported".formatted(eventType));
    };
  }

}
