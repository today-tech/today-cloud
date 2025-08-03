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

public class RequestStreamFrameCodec {

  private RequestStreamFrameCodec() { }

  public static ByteBuf encodeReleasingPayload(
          ByteBufAllocator allocator, int streamId, long initialRequestN, Payload payload) {

    if (initialRequestN < 1) {
      throw new IllegalArgumentException("request n is less than 1");
    }

    int reqN = initialRequestN > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) initialRequestN;

    return GenericFrameCodec.encodeReleasingPayload(
            allocator, FrameType.REQUEST_STREAM, streamId, false, false, reqN, payload);
  }

  public static ByteBuf encode(
          ByteBufAllocator allocator,
          int streamId,
          boolean fragmentFollows,
          long initialRequestN,
          @Nullable ByteBuf metadata,
          ByteBuf data) {

    if (initialRequestN < 1) {
      throw new IllegalArgumentException("request n is less than 1");
    }

    int reqN = initialRequestN > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) initialRequestN;

    return GenericFrameCodec.encode(
            allocator,
            FrameType.REQUEST_STREAM,
            streamId,
            fragmentFollows,
            false,
            false,
            reqN,
            metadata,
            data);
  }

  public static ByteBuf data(ByteBuf byteBuf) {
    return GenericFrameCodec.dataWithRequestN(byteBuf);
  }

  @Nullable
  public static ByteBuf metadata(ByteBuf byteBuf) {
    return GenericFrameCodec.metadataWithRequestN(byteBuf);
  }

  public static long initialRequestN(ByteBuf byteBuf) {
    int requestN = GenericFrameCodec.initialRequestN(byteBuf);
    return requestN == Integer.MAX_VALUE ? Long.MAX_VALUE : requestN;
  }
}
