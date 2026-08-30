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

package infra.remoting.transport.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import static infra.remoting.frame.FrameLengthCodec.FRAME_LENGTH_MASK;
import static infra.remoting.frame.FrameLengthCodec.FRAME_LENGTH_SIZE;

/**
 * An extension to the Netty {@link LengthFieldBasedFrameDecoder} that encapsulates the
 * specific frame length header details.
 */
public final class ProtocolFrameLengthCodec extends LengthFieldBasedFrameDecoder {

  /**
   * Creates a new instance of the decoder, specifying the protocol frame length header size.
   */
  public ProtocolFrameLengthCodec() {
    this(FRAME_LENGTH_MASK);
  }

  /**
   * Creates a new instance of the decoder, specifying the Protocol frame length header size.
   *
   * @param maxFrameLength maximum allowed frame length for incoming protocol frames
   */
  public ProtocolFrameLengthCodec(int maxFrameLength) {
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
