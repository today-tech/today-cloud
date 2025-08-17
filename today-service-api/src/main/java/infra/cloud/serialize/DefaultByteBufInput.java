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

package infra.cloud.serialize;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/12/20 21:35
 */
public class DefaultByteBufInput implements Input {

  private final ByteBuf buffer;

  public DefaultByteBufInput(ByteBuf buffer) {
    this.buffer = buffer;
  }

  @Override
  public void read(byte[] b) {
    buffer.readBytes(b);
  }

  @Override
  public byte[] read() {
    return ByteBufUtil.getBytes(buffer);
  }

  @Override
  public void read(byte[] b, int off, int len) {
    buffer.readBytes(b, off, len);
  }

  @Override
  public int skipBytes(int n) {
    int start = buffer.readerIndex();
    buffer.skipBytes(n);
    return start - buffer.readerIndex();
  }

  @Override
  public boolean readBoolean() {
    return buffer.readBoolean();
  }

  @Override
  public byte readByte() {
    return buffer.readByte();
  }

  @Override
  public int readUnsignedByte() {
    return buffer.readUnsignedByte();
  }

  @Override
  public short readShort() {
    return buffer.readShort();
  }

  @Override
  public int readUnsignedShort() {
    return buffer.readUnsignedShort();
  }

  @Override
  public char readChar() {
    return buffer.readChar();
  }

  @Override
  public int readInt() {
    return buffer.readInt();
  }

  @Override
  public long readLong() {
    return buffer.readLong();
  }

  @Override
  public float readFloat() {
    return buffer.readFloat();
  }

  @Override
  public double readDouble() {
    return buffer.readDouble();
  }

  @Override
  public String readString() {
    return buffer.readString(readUnsignedShort(), StandardCharsets.UTF_8);
  }

}
