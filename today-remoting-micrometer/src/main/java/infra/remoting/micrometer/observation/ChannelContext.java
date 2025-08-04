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

package infra.remoting.micrometer.observation;

import infra.lang.Nullable;
import infra.remoting.Payload;
import infra.remoting.frame.FrameType;
import io.micrometer.observation.Observation;
import io.netty.buffer.ByteBuf;

public class ChannelContext extends Observation.Context {

  final Payload payload;

  final ByteBuf metadata;

  final FrameType frameType;

  final String route;

  final Side side;

  Payload modifiedPayload;

  ChannelContext(Payload payload, ByteBuf metadata,
          FrameType frameType, @Nullable String route, Side side) {
    this.payload = payload;
    this.metadata = metadata;
    this.frameType = frameType;
    this.route = route;
    this.side = side;
  }

  public enum Side {
    REQUESTER,
    RESPONDER
  }

  public Payload getPayload() {
    return payload;
  }

  public ByteBuf getMetadata() {
    return metadata;
  }

  public FrameType getFrameType() {
    return frameType;
  }

  public String getRoute() {
    return route;
  }

  public Side getSide() {
    return side;
  }

  public Payload getModifiedPayload() {
    return modifiedPayload;
  }
}
