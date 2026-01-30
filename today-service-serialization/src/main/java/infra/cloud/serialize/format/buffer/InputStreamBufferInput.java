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
