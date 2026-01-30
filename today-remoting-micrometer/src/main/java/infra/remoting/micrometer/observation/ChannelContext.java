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

package infra.remoting.micrometer.observation;

import org.jspecify.annotations.Nullable;

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
