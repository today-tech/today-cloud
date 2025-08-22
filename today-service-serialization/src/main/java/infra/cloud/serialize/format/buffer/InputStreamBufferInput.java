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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.Objects;

import infra.lang.Assert;

/**
 * {@link MessageBufferInput} adapter for {@link InputStream}
 */
public class InputStreamBufferInput implements MessageBufferInput {

  private InputStream in;

  private final byte[] buffer;

  public static MessageBufferInput newBufferInput(InputStream in) {
    Assert.notNull(in, "InputStream is null");
    if (in instanceof FileInputStream) {
      FileChannel channel = ((FileInputStream) in).getChannel();
      if (channel != null) {
        return new ChannelBufferInput(channel);
      }
    }
    return new InputStreamBufferInput(in);
  }

  public InputStreamBufferInput(InputStream in) {
    this(in, 8192);
  }

  public InputStreamBufferInput(InputStream in, int bufferSize) {
    this.in = Objects.requireNonNull(in, "input is null");
    this.buffer = new byte[bufferSize];
  }

  /**
   * Reset Stream. This method doesn't close the old resource.
   *
   * @param in new stream
   * @return the old resource
   */
  public InputStream reset(InputStream in) throws IOException {
    InputStream old = this.in;
    this.in = in;
    return old;
  }

  @Override
  public MessageBuffer next() throws IOException {
    int readLen = in.read(buffer);
    if (readLen == -1) {
      return null;
    }
    return MessageBuffer.wrap(buffer, 0, readLen);
  }

  @Override
  public void close()
          throws IOException {
    in.close();
  }
}
