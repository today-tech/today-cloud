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

public class RequestFireAndForgetFrameCodec {

  private RequestFireAndForgetFrameCodec() { }

  public static ByteBuf encodeReleasingPayload(
          ByteBufAllocator allocator, int streamId, Payload payload) {

    return GenericFrameCodec.encodeReleasingPayload(
            allocator, FrameType.REQUEST_FNF, streamId, false, false, payload);
  }

  public static ByteBuf encode(
          ByteBufAllocator allocator,
          int streamId,
          boolean fragmentFollows,
          @Nullable ByteBuf metadata,
          ByteBuf data) {

    return GenericFrameCodec.encode(
            allocator, FrameType.REQUEST_FNF, streamId, fragmentFollows, metadata, data);
  }

  public static ByteBuf data(ByteBuf byteBuf) {
    return GenericFrameCodec.data(byteBuf);
  }

  @Nullable
  public static ByteBuf metadata(ByteBuf byteBuf) {
    return GenericFrameCodec.metadata(byteBuf);
  }
}
