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

public class PayloadFrameCodec {

  private PayloadFrameCodec() { }

  public static ByteBuf encodeNextReleasingPayload(
          ByteBufAllocator allocator, int streamId, Payload payload) {

    return encodeReleasingPayload(allocator, streamId, false, payload);
  }

  public static ByteBuf encodeNextCompleteReleasingPayload(
          ByteBufAllocator allocator, int streamId, Payload payload) {

    return encodeReleasingPayload(allocator, streamId, true, payload);
  }

  static ByteBuf encodeReleasingPayload(
          ByteBufAllocator allocator, int streamId, boolean complete, Payload payload) {

    return GenericFrameCodec.encodeReleasingPayload(
            allocator, FrameType.PAYLOAD, streamId, complete, true, payload);
  }

  public static ByteBuf encodeComplete(ByteBufAllocator allocator, int streamId) {
    return encode(allocator, streamId, false, true, false, null, null);
  }

  public static ByteBuf encode(
          ByteBufAllocator allocator,
          int streamId,
          boolean fragmentFollows,
          boolean complete,
          boolean next,
          @Nullable ByteBuf metadata,
          @Nullable ByteBuf data) {

    return GenericFrameCodec.encode(
            allocator, FrameType.PAYLOAD, streamId, fragmentFollows, complete, next, 0, metadata, data);
  }

  public static ByteBuf data(ByteBuf byteBuf) {
    return GenericFrameCodec.data(byteBuf);
  }

  @Nullable
  public static ByteBuf metadata(ByteBuf byteBuf) {
    return GenericFrameCodec.metadata(byteBuf);
  }
}
