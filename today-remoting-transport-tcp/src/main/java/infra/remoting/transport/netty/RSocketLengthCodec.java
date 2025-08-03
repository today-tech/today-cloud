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

package infra.remoting.transport.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import static infra.remoting.frame.FrameLengthCodec.FRAME_LENGTH_MASK;
import static infra.remoting.frame.FrameLengthCodec.FRAME_LENGTH_SIZE;

/**
 * An extension to the Netty {@link LengthFieldBasedFrameDecoder} that encapsulates the
 * RSocket-specific frame length header details.
 */
public final class RSocketLengthCodec extends LengthFieldBasedFrameDecoder {

  /** Creates a new instance of the decoder, specifying the RSocket frame length header size. */
  public RSocketLengthCodec() {
    this(FRAME_LENGTH_MASK);
  }

  /**
   * Creates a new instance of the decoder, specifying the RSocket frame length header size.
   *
   * @param maxFrameLength maximum allowed frame length for incoming rsocket frames
   */
  public RSocketLengthCodec(int maxFrameLength) {
    super(maxFrameLength, 0, FRAME_LENGTH_SIZE, 0, 0);
  }

  /**
   * Simplified non-netty focused decode usage.
   *
   * @param in the input buffer to read data from.
   * @return decoded buffer or null is none available.
   * @throws Exception if any error happens.
   * @see #decode(ChannelHandlerContext, ByteBuf)
   */
  public Object decode(ByteBuf in) throws Exception {
    return decode(null, in);
  }
}
