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

package infra.remoting.frame;

import infra.lang.Nullable;
import infra.remoting.Payload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public class RequestResponseFrameCodec {

  private RequestResponseFrameCodec() { }

  public static ByteBuf encodeReleasingPayload(
          ByteBufAllocator allocator, int streamId, Payload payload) {

    return GenericFrameCodec.encodeReleasingPayload(
            allocator, FrameType.REQUEST_RESPONSE, streamId, false, false, payload);
  }

  public static ByteBuf encode(
          ByteBufAllocator allocator,
          int streamId,
          boolean fragmentFollows,
          @Nullable ByteBuf metadata,
          ByteBuf data) {
    return GenericFrameCodec.encode(
            allocator, FrameType.REQUEST_RESPONSE, streamId, fragmentFollows, metadata, data);
  }

  public static ByteBuf data(ByteBuf byteBuf) {
    return GenericFrameCodec.data(byteBuf);
  }

  @Nullable
  public static ByteBuf metadata(ByteBuf byteBuf) {
    return GenericFrameCodec.metadata(byteBuf);
  }
}
