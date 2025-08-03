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

package io.rsocket.frame;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.rsocket.RSocketErrorException;

public class ErrorFrameCodec {

  // defined zero stream id error codes
  public static final int INVALID_SETUP = 0x00000001;
  public static final int UNSUPPORTED_SETUP = 0x00000002;
  public static final int REJECTED_SETUP = 0x00000003;
  public static final int REJECTED_RESUME = 0x00000004;
  public static final int CONNECTION_ERROR = 0x00000101;
  public static final int CONNECTION_CLOSE = 0x00000102;
  // defined non-zero stream id error codes
  public static final int APPLICATION_ERROR = 0x00000201;
  public static final int REJECTED = 0x00000202;
  public static final int CANCELED = 0x00000203;
  public static final int INVALID = 0x00000204;
  // defined user-allowed error codes range
  public static final int MIN_USER_ALLOWED_ERROR_CODE = 0x00000301;
  public static final int MAX_USER_ALLOWED_ERROR_CODE = 0xFFFFFFFE;

  public static ByteBuf encode(ByteBufAllocator allocator, int streamId, Throwable t, ByteBuf data) {
    ByteBuf header = FrameHeaderCodec.encode(allocator, streamId, FrameType.ERROR, 0);

    int errorCode = t instanceof RSocketErrorException
            ? ((RSocketErrorException) t).errorCode()
            : APPLICATION_ERROR;

    header.writeInt(errorCode);

    return allocator.compositeBuffer(2).addComponents(true, header, data);
  }

  public static ByteBuf encode(ByteBufAllocator allocator, int streamId, Throwable t) {
    String message = t.getMessage() == null ? "" : t.getMessage();
    ByteBuf data = ByteBufUtil.writeUtf8(allocator, message);
    return encode(allocator, streamId, t, data);
  }

  public static int errorCode(ByteBuf byteBuf) {
    byteBuf.markReaderIndex();
    byteBuf.skipBytes(FrameHeaderCodec.size());
    int i = byteBuf.readInt();
    byteBuf.resetReaderIndex();
    return i;
  }

  public static ByteBuf data(ByteBuf byteBuf) {
    byteBuf.markReaderIndex();
    byteBuf.skipBytes(FrameHeaderCodec.size() + Integer.BYTES);
    ByteBuf slice = byteBuf.slice();
    byteBuf.resetReaderIndex();
    return slice;
  }

  public static String dataUtf8(ByteBuf byteBuf) {
    return data(byteBuf).toString(StandardCharsets.UTF_8);
  }
}
