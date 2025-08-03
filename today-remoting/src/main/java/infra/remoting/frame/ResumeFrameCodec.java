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

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

public class ResumeFrameCodec {
  static final int CURRENT_VERSION = SetupFrameCodec.CURRENT_VERSION;

  public static ByteBuf encode(
          ByteBufAllocator allocator,
          ByteBuf token,
          long lastReceivedServerPos,
          long firstAvailableClientPos) {

    ByteBuf byteBuf = FrameHeaderCodec.encodeStreamZero(allocator, FrameType.RESUME, 0);
    byteBuf.writeInt(CURRENT_VERSION);
    token.markReaderIndex();
    byteBuf.writeShort(token.readableBytes());
    byteBuf.writeBytes(token);
    token.resetReaderIndex();
    byteBuf.writeLong(lastReceivedServerPos);
    byteBuf.writeLong(firstAvailableClientPos);

    return byteBuf;
  }

  public static int version(ByteBuf byteBuf) {
    FrameHeaderCodec.ensureFrameType(FrameType.RESUME, byteBuf);

    byteBuf.markReaderIndex();
    byteBuf.skipBytes(FrameHeaderCodec.size());
    int version = byteBuf.readInt();
    byteBuf.resetReaderIndex();

    return version;
  }

  public static ByteBuf token(ByteBuf byteBuf) {
    FrameHeaderCodec.ensureFrameType(FrameType.RESUME, byteBuf);

    byteBuf.markReaderIndex();
    // header + version
    int tokenPos = FrameHeaderCodec.size() + Integer.BYTES;
    byteBuf.skipBytes(tokenPos);
    // token
    int tokenLength = byteBuf.readShort() & 0xFFFF;
    ByteBuf token = byteBuf.readSlice(tokenLength);
    byteBuf.resetReaderIndex();

    return token;
  }

  public static long lastReceivedServerPos(ByteBuf byteBuf) {
    FrameHeaderCodec.ensureFrameType(FrameType.RESUME, byteBuf);

    byteBuf.markReaderIndex();
    // header + version
    int tokenPos = FrameHeaderCodec.size() + Integer.BYTES;
    byteBuf.skipBytes(tokenPos);
    // token
    int tokenLength = byteBuf.readShort() & 0xFFFF;
    byteBuf.skipBytes(tokenLength);
    long lastReceivedServerPos = byteBuf.readLong();
    byteBuf.resetReaderIndex();

    return lastReceivedServerPos;
  }

  public static long firstAvailableClientPos(ByteBuf byteBuf) {
    FrameHeaderCodec.ensureFrameType(FrameType.RESUME, byteBuf);

    byteBuf.markReaderIndex();
    // header + version
    int tokenPos = FrameHeaderCodec.size() + Integer.BYTES;
    byteBuf.skipBytes(tokenPos);
    // token
    int tokenLength = byteBuf.readShort() & 0xFFFF;
    byteBuf.skipBytes(tokenLength);
    // last received server position
    byteBuf.skipBytes(Long.BYTES);
    long firstAvailableClientPos = byteBuf.readLong();
    byteBuf.resetReaderIndex();

    return firstAvailableClientPos;
  }

  public static ByteBuf generateResumeToken() {
    UUID uuid = UUID.randomUUID();
    ByteBuf bb = Unpooled.buffer(16);
    bb.writeLong(uuid.getMostSignificantBits());
    bb.writeLong(uuid.getLeastSignificantBits());
    return bb;
  }
}
