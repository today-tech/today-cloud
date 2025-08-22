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

package infra.remoting.frame.decoder;

import java.nio.ByteBuffer;

import infra.remoting.Payload;
import infra.remoting.frame.FrameHeaderCodec;
import infra.remoting.frame.FrameType;
import infra.remoting.frame.MetadataPushFrameCodec;
import infra.remoting.frame.PayloadFrameCodec;
import infra.remoting.frame.RequestChannelFrameCodec;
import infra.remoting.frame.RequestFireAndForgetFrameCodec;
import infra.remoting.frame.RequestResponseFrameCodec;
import infra.remoting.frame.RequestStreamFrameCodec;
import infra.remoting.util.DefaultPayload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Default Frame decoder that copies the frames contents for easy of use.
 */
class DefaultPayloadDecoder implements PayloadDecoder {

  @Override
  public Payload decode(ByteBuf byteBuf) {
    ByteBuf d;
    FrameType type = FrameHeaderCodec.frameType(byteBuf);
    ByteBuf m = switch (type) {
      case REQUEST_FNF -> {
        d = RequestFireAndForgetFrameCodec.data(byteBuf);
        yield RequestFireAndForgetFrameCodec.metadata(byteBuf);
      }
      case REQUEST_RESPONSE -> {
        d = RequestResponseFrameCodec.data(byteBuf);
        yield RequestResponseFrameCodec.metadata(byteBuf);
      }
      case REQUEST_STREAM -> {
        d = RequestStreamFrameCodec.data(byteBuf);
        yield RequestStreamFrameCodec.metadata(byteBuf);
      }
      case REQUEST_CHANNEL -> {
        d = RequestChannelFrameCodec.data(byteBuf);
        yield RequestChannelFrameCodec.metadata(byteBuf);
      }
      case NEXT, NEXT_COMPLETE -> {
        d = PayloadFrameCodec.data(byteBuf);
        yield PayloadFrameCodec.metadata(byteBuf);
      }
      case METADATA_PUSH -> {
        d = Unpooled.EMPTY_BUFFER;
        yield MetadataPushFrameCodec.metadata(byteBuf);
      }
      default -> throw new IllegalArgumentException("unsupported frame type: " + type);
    };

    ByteBuffer data = ByteBuffer.allocate(d.readableBytes());
    data.put(d.nioBuffer());
    data.flip();

    if (m != null) {
      ByteBuffer metadata = ByteBuffer.allocate(m.readableBytes());
      metadata.put(m.nioBuffer());
      metadata.flip();

      return DefaultPayload.create(data, metadata);
    }

    return DefaultPayload.create(data);
  }
}
