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

package infra.remoting.frame;

import org.jspecify.annotations.Nullable;

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
