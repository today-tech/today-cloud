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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

public class FrameUtil {

  private FrameUtil() { }

  public static String toString(ByteBuf frame) {
    FrameType frameType = FrameHeaderCodec.frameType(frame);
    int streamId = FrameHeaderCodec.streamId(frame);
    StringBuilder payload = new StringBuilder();

    payload
            .append("\nFrame => Stream ID: ")
            .append(streamId)
            .append(" Type: ")
            .append(frameType)
            .append(" Flags: 0b")
            .append(Integer.toBinaryString(FrameHeaderCodec.flags(frame)))
            .append(" Length: " + frame.readableBytes());

    if (frameType.hasInitialRequestN()) {
      payload.append(" InitialRequestN: ").append(RequestStreamFrameCodec.initialRequestN(frame));
    }

    if (frameType == FrameType.REQUEST_N) {
      payload.append(" RequestN: ").append(RequestNFrameCodec.requestN(frame));
    }

    if (FrameHeaderCodec.hasMetadata(frame)) {
      payload.append("\nMetadata:\n");

      ByteBufUtil.appendPrettyHexDump(payload, getMetadata(frame, frameType));
    }

    payload.append("\nData:\n");
    ByteBufUtil.appendPrettyHexDump(payload, getData(frame, frameType));

    return payload.toString();
  }

  private static ByteBuf getMetadata(ByteBuf frame, FrameType frameType) {
    boolean hasMetadata = FrameHeaderCodec.hasMetadata(frame);
    if (hasMetadata) {
      ByteBuf metadata;
      switch (frameType) {
        case REQUEST_FNF:
          metadata = RequestFireAndForgetFrameCodec.metadata(frame);
          break;
        case REQUEST_STREAM:
          metadata = RequestStreamFrameCodec.metadata(frame);
          break;
        case REQUEST_RESPONSE:
          metadata = RequestResponseFrameCodec.metadata(frame);
          break;
        case REQUEST_CHANNEL:
          metadata = RequestChannelFrameCodec.metadata(frame);
          break;
        // Payload and synthetic types
        case PAYLOAD:
        case NEXT:
        case NEXT_COMPLETE:
        case COMPLETE:
          metadata = PayloadFrameCodec.metadata(frame);
          break;
        case METADATA_PUSH:
          metadata = MetadataPushFrameCodec.metadata(frame);
          break;
        case SETUP:
          metadata = SetupFrameCodec.metadata(frame);
          break;
        case LEASE:
          metadata = LeaseFrameCodec.metadata(frame);
          break;
        default:
          return Unpooled.EMPTY_BUFFER;
      }
      return metadata;
    }
    else {
      return Unpooled.EMPTY_BUFFER;
    }
  }

  private static ByteBuf getData(ByteBuf frame, FrameType frameType) {
    ByteBuf data;
    switch (frameType) {
      case REQUEST_FNF:
        data = RequestFireAndForgetFrameCodec.data(frame);
        break;
      case REQUEST_STREAM:
        data = RequestStreamFrameCodec.data(frame);
        break;
      case REQUEST_RESPONSE:
        data = RequestResponseFrameCodec.data(frame);
        break;
      case REQUEST_CHANNEL:
        data = RequestChannelFrameCodec.data(frame);
        break;
      // Payload, KeepAlive and synthetic types
      case PAYLOAD:
      case KEEPALIVE:
      case NEXT:
      case NEXT_COMPLETE:
      case COMPLETE:
        data = PayloadFrameCodec.data(frame);
        break;
      case SETUP:
        data = SetupFrameCodec.data(frame);
        break;
      default:
        return Unpooled.EMPTY_BUFFER;
    }
    return data;
  }
}
