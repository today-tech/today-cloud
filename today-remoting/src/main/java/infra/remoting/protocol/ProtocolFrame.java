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

package infra.remoting.protocol;

import java.util.concurrent.Flow;

import infra.lang.Nullable;
import infra.remoting.Payload;
import infra.remoting.frame.FrameType;
import infra.remoting.frame.decoder.PayloadDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

/**
 * Protocol frame
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/12/22 22:29
 */
public class ProtocolFrame {

  /** (I)gnore flag: a value of 0 indicates the protocol can't ignore this frame */
  public static final int FLAGS_I = 0b10_0000_0000;
  /** (M)etadata flag: a value of 1 indicates the frame contains metadata */
  public static final int FLAGS_M = 0b01_0000_0000;
  /**
   * (F)ollows: More fragments follow this fragment (in case of fragmented REQUEST_x or PAYLOAD
   * frames)
   */
  public static final int FLAGS_F = 0b00_1000_0000;
  /** (C)omplete: bit to indicate stream completion ({@link Flow.Subscriber#onComplete()}) */
  public static final int FLAGS_C = 0b00_0100_0000;
  /** (N)ext: bit to indicate payload or metadata present ({@link Flow.Subscriber#onNext(Object)}) */
  public static final int FLAGS_N = 0b00_0010_0000;

  private static final int FRAME_FLAGS_MASK = 0b0000_0011_1111_1111;
  private static final int FRAME_TYPE_BITS = 6;
  private static final int FRAME_TYPE_SHIFT = 16 - FRAME_TYPE_BITS;

  private static final int HEADER_SIZE = Integer.BYTES + Short.BYTES;

  public final int streamId;

  public final FrameType frameType;

  public final Payload payload;

  private final int flags;

  private final int typeAndFlags;

  @Nullable
  private FrameType syntheticFrameType;

  public ProtocolFrame(int streamId, FrameType frameType, int flags, Payload payload) {
    this.streamId = streamId;
    this.frameType = frameType;
    this.flags = flags;
    this.payload = payload;
    this.typeAndFlags = 0;
  }

  public ProtocolFrame(int streamId, FrameType frameType, int flags, int typeAndFlags, Payload payload) {
    this.streamId = streamId;
    this.frameType = frameType;
    this.flags = flags;
    this.typeAndFlags = typeAndFlags;
    this.payload = payload;
  }

  public int getStreamId() {
    return streamId;
  }

  public FrameType syntheticFrameType() {
    FrameType result = this.syntheticFrameType;
    if (result == null) {
      result = frameType;
      if (result == FrameType.PAYLOAD) {
        boolean next = hasFlag(FLAGS_N);
        boolean complete = hasFlag(FLAGS_C);
        if (next && complete) {
          result = FrameType.NEXT_COMPLETE;
        }
        else if (complete) {
          result = FrameType.COMPLETE;
        }
        else if (next) {
          result = FrameType.NEXT;
        }
        else {
          throw new IllegalArgumentException("Payload must set either or both of NEXT and COMPLETE.");
        }
      }
      this.syntheticFrameType = result;
    }
    return result;
  }

  public boolean hasMetadata() {
    return payload.hasMetadata();
  }

  public boolean hasFollows() {
    return hasFlag(flags, FLAGS_F);
  }

  public boolean hasComplete() {
    return hasFlag(flags, FLAGS_C);
  }

  public boolean hasFlag(int flag) {
    return (flags & flag) == flag;
  }

  public int getLength() {
    return HEADER_SIZE + payload.length();
  }

  public void release() {
    if (payload != null) {
      payload.release();
    }
  }

  public ByteBuf serialize(ByteBufAllocator allocator) {
    if (!frameType.canHaveMetadata() && hasFlag(FLAGS_M)) {
      throw new IllegalStateException("bad value for metadata flag");
    }

    short typeAndFlags = (short) (frameType.getEncodedType() << FRAME_TYPE_SHIFT | (short) flags);
    return allocator.buffer().writeInt(streamId).writeShort(typeAndFlags);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("\nFrame => Stream ID: ")
            .append(streamId)
            .append(" Type: ")
            .append(frameType)
            .append(" Flags: 0b")
            .append(Integer.toBinaryString(flags))
            .append(" Length: ")
            .append(getLength());

    if (hasMetadata()) {
      builder.append("\nMetadata:\n");
      ByteBufUtil.appendPrettyHexDump(builder, payload.metadata());
    }

    builder.append("\nData:\n");
    if (payload != null) {
      ByteBufUtil.appendPrettyHexDump(builder, payload.data());
    }
    else {
      ByteBufUtil.appendPrettyHexDump(builder, Unpooled.EMPTY_BUFFER);
    }
    return builder.toString();
  }

  public static boolean hasFlag(int flags, int flag) {
    return (flags & flag) == flag;
  }

  /**
   * Parsing given buffer into {@link ProtocolFrame}
   *
   * @param frame frame buffer
   * @throws ProtocolParsingException protocol parsing errors
   */
  public static ProtocolFrame parse(ByteBuf frame, PayloadDecoder decoder) throws ProtocolParsingException {
    int streamId = frame.readInt();
    int typeAndFlags = frame.readShort() & 0xFFFF;
    FrameType nativeFrameType = FrameType.forEncodedType(typeAndFlags >> FRAME_TYPE_SHIFT);
    final int flags = typeAndFlags & FRAME_FLAGS_MASK;

    Payload payload = decoder.decode(frame);
    return new ProtocolFrame(streamId, nativeFrameType, flags, typeAndFlags, payload);
  }

}
