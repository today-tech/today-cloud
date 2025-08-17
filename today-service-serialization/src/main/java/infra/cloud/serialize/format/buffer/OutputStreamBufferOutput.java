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
import java.io.OutputStream;
import java.util.Objects;

/**
 * MessageBufferOutput adapter for {@link OutputStream}.
 */
public class OutputStreamBufferOutput implements MessageBufferOutput {

  private OutputStream out;

  private MessageBuffer buffer;

  public OutputStreamBufferOutput(OutputStream out) {
    this(out, 8192);
  }

  public OutputStreamBufferOutput(OutputStream out, int bufferSize) {
    this.out = Objects.requireNonNull(out, "output is null");
    this.buffer = MessageBuffer.allocate(bufferSize);
  }

  /**
   * Reset Stream. This method doesn't close the old stream.
   *
   * @param out new stream
   * @return the old stream
   */
  public OutputStream reset(OutputStream out) {
    OutputStream old = this.out;
    this.out = out;
    return old;
  }

  @Override
  public MessageBuffer next(int minimumSize) throws IOException {
    if (buffer.size() < minimumSize) {
      buffer = MessageBuffer.allocate(minimumSize);
    }
    return buffer;
  }

  @Override
  public void writeBuffer(int length) throws IOException {
    write(buffer.array(), buffer.arrayOffset(), length);
  }

  @Override
  public void write(byte[] buffer, int offset, int length) throws IOException {
    out.write(buffer, offset, length);
  }

  @Override
  public void add(byte[] buffer, int offset, int length) throws IOException {
    write(buffer, offset, length);
  }

  @Override
  public void close() throws IOException {
    out.close();
  }

  @Override
  public void flush() throws IOException {
    out.flush();
  }
}
