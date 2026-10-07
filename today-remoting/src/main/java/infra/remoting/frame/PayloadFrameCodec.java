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
