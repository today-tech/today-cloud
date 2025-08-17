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

package infra.cloud.serialize.format.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Objects;

import infra.lang.Assert;

/**
 * {@link MessageBufferInput} adapter for {@link ReadableByteChannel}
 */
public class ChannelBufferInput implements MessageBufferInput {

  private ReadableByteChannel channel;

  private final MessageBuffer buffer;

  public ChannelBufferInput(ReadableByteChannel channel) {
    this(channel, 8192);
  }

  public ChannelBufferInput(ReadableByteChannel channel, int bufferSize) {
    this.channel = Objects.requireNonNull(channel, "input channel is null");
    Assert.isTrue(bufferSize > 0, () -> "buffer size must be > 0: " + bufferSize);
    this.buffer = MessageBuffer.allocate(bufferSize);
  }

  /**
   * Reset channel. This method doesn't close the old resource.
   *
   * @param channel new channel
   * @return the old resource
   */
  public ReadableByteChannel reset(ReadableByteChannel channel) throws IOException {
    ReadableByteChannel old = this.channel;
    this.channel = channel;
    return old;
  }

  @Override
  public MessageBuffer next() throws IOException {
    ByteBuffer b = buffer.sliceAsByteBuffer();
    int ret = channel.read(b);
    if (ret == -1) {
      return null;
    }
    b.flip();
    return buffer.slice(0, b.limit());
  }

  @Override
  public void close() throws IOException {
    channel.close();
  }
}
