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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public class RequestNFrameCodec {
  private RequestNFrameCodec() { }

  public static ByteBuf encode(
          final ByteBufAllocator allocator, final int streamId, long requestN) {

    if (requestN < 1) {
      throw new IllegalArgumentException("request n is less than 1");
    }

    int reqN = requestN > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) requestN;

    ByteBuf header = FrameHeaderCodec.encode(allocator, streamId, FrameType.REQUEST_N, 0);
    return header.writeInt(reqN);
  }

  public static long requestN(ByteBuf byteBuf) {
    FrameHeaderCodec.ensureFrameType(FrameType.REQUEST_N, byteBuf);
    byteBuf.markReaderIndex();
    byteBuf.skipBytes(FrameHeaderCodec.size());
    int i = byteBuf.readInt();
    byteBuf.resetReaderIndex();
    return i == Integer.MAX_VALUE ? Long.MAX_VALUE : i;
  }
}
