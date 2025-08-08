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
import io.protostuff.Output;
import io.protostuff.Schema;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/12/20 21:28
 */
public class ByteBufOutput implements Output {
  private final ByteBuf buffer;

  public ByteBufOutput(ByteBuf buffer) {
    this.buffer = buffer;
  }

  @Override
  public void writeInt32(int fieldNumber, int value, boolean repeated) throws IOException {

  }

  @Override
  public void writeUInt32(int fieldNumber, int value, boolean repeated) throws IOException {

  }

  @Override
  public void writeSInt32(int fieldNumber, int value, boolean repeated) throws IOException {

  }

  @Override
  public void writeFixed32(int fieldNumber, int value, boolean repeated) throws IOException {

  }

  @Override
  public void writeSFixed32(int fieldNumber, int value, boolean repeated) throws IOException {

  }

  @Override
  public void writeInt64(int fieldNumber, long value, boolean repeated) throws IOException {

  }

  @Override
  public void writeUInt64(int fieldNumber, long value, boolean repeated) throws IOException {

  }

  @Override
  public void writeSInt64(int fieldNumber, long value, boolean repeated) throws IOException {

  }

  @Override
  public void writeFixed64(int fieldNumber, long value, boolean repeated) throws IOException {

  }

  @Override
  public void writeSFixed64(int fieldNumber, long value, boolean repeated) throws IOException {

  }

  @Override
  public void writeFloat(int fieldNumber, float value, boolean repeated) throws IOException {

  }

  @Override
  public void writeDouble(int fieldNumber, double value, boolean repeated) throws IOException {

  }

  @Override
  public void writeBool(int fieldNumber, boolean value, boolean repeated) throws IOException {

  }

  @Override
  public void writeEnum(int fieldNumber, int value, boolean repeated) throws IOException {

  }

  @Override
  public void writeString(int fieldNumber, CharSequence value, boolean repeated) throws IOException {

  }

  @Override
  public void writeBytes(int fieldNumber, ByteString value, boolean repeated) throws IOException {

  }

  @Override
  public void writeByteArray(int fieldNumber, byte[] value, boolean repeated) throws IOException {

  }

  @Override
  public void writeByteRange(boolean utf8String, int fieldNumber, byte[] value, int offset, int length, boolean repeated) throws IOException {

  }

  @Override
  public <T> void writeObject(int fieldNumber, T value, Schema<T> schema, boolean repeated) throws IOException {

  }

  @Override
  public void writeBytes(int fieldNumber, ByteBuffer value, boolean repeated) throws IOException {

  }
}
