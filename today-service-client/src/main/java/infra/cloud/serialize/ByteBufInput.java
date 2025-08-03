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

import java.io.IOException;
import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import io.protostuff.ByteString;
import io.protostuff.Input;
import io.protostuff.Output;
import io.protostuff.Schema;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/12/20 21:35
 */
public class ByteBufInput implements Input {

  private final ByteBuf buffer;

  public ByteBufInput(ByteBuf buffer) {
    this.buffer = buffer;
  }

  @Override
  public <T> void handleUnknownField(int fieldNumber, Schema<T> schema) throws IOException {

  }

  @Override
  public <T> int readFieldNumber(Schema<T> schema) throws IOException {
    return 0;
  }

  @Override
  public int readInt32() throws IOException {
    return 0;
  }

  @Override
  public int readUInt32() throws IOException {
    return 0;
  }

  @Override
  public int readSInt32() throws IOException {
    return 0;
  }

  @Override
  public int readFixed32() throws IOException {
    return 0;
  }

  @Override
  public int readSFixed32() throws IOException {
    return 0;
  }

  @Override
  public long readInt64() throws IOException {
    return 0;
  }

  @Override
  public long readUInt64() throws IOException {
    return 0;
  }

  @Override
  public long readSInt64() throws IOException {
    return 0;
  }

  @Override
  public long readFixed64() throws IOException {
    return 0;
  }

  @Override
  public long readSFixed64() throws IOException {
    return 0;
  }

  @Override
  public float readFloat() throws IOException {
    return 0;
  }

  @Override
  public double readDouble() throws IOException {
    return 0;
  }

  @Override
  public boolean readBool() throws IOException {
    return false;
  }

  @Override
  public int readEnum() throws IOException {
    return 0;
  }

  @Override
  public String readString() throws IOException {
    return "";
  }

  @Override
  public ByteString readBytes() throws IOException {
    return null;
  }

  @Override
  public void readBytes(ByteBuffer bb) throws IOException {

  }

  @Override
  public byte[] readByteArray() throws IOException {
    return new byte[0];
  }

  @Override
  public ByteBuffer readByteBuffer() throws IOException {
    return null;
  }

  @Override
  public <T> T mergeObject(T value, Schema<T> schema) throws IOException {
    return null;
  }

  @Override
  public void transferByteRangeTo(Output output, boolean utf8String, int fieldNumber, boolean repeated) throws IOException {

  }
}
