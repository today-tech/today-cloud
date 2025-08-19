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

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import infra.lang.Constant;
import infra.util.CollectionUtils;
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
    return read(buffer.readInt());
  }

  @Override
  public void read(byte[] b, int off, int len) {
    buffer.readBytes(b, off, len);
  }

  @Override
  public byte[] read(int len) {
    if (len == 0) {
      return Constant.EMPTY_BYTES;
    }
    return ByteBufUtil.getBytes(buffer, buffer.readerIndex(), len);
  }

  @Override
  public byte[] readFully() {
    return ByteBufUtil.getBytes(buffer);
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
    int length = readUnsignedShort();
    if (length == 0) {
      return Constant.BLANK;
    }
    byte[] array = read(length);
    return new String(array, StandardCharsets.UTF_8);
  }

  @Override
  public void read(Message message) {
    message.readFrom(this);
  }

  @Override
  public Instant readTimestamp() {
    return Instant.ofEpochSecond(buffer.readLong(), buffer.readInt());
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T[] read(Class<T> type, Function<Input, T> mapper) {
    int size = readInt();
    T[] array = (T[]) Array.newInstance(type, size);
    for (int i = 0; i < size; i++) {
      array[i] = mapper.apply(this);
    }
    return array;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T[] read(Class<T> type, Supplier<T> supplier) {
    int size = readInt();
    T[] array = (T[]) Array.newInstance(type, size);
    for (int i = 0; i < size; i++) {
      array[i] = supplier.get();
    }
    return array;
  }

  @Override
  public <T> List<T> read(Function<Input, T> mapper) {
    int size = readInt();
    ArrayList<T> result = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      result.add(mapper.apply(this));
    }
    return result;
  }

  @Override
  public <T> List<T> read(Supplier<T> supplier) {
    int size = readInt();
    ArrayList<T> result = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      result.add(supplier.get());
    }
    return result;
  }

  @Override
  public <K, V> Map<K, V> read(Function<Input, K> keyMapper, Function<Input, V> valueMapper) {
    int size = readInt();
    LinkedHashMap<K, V> result = CollectionUtils.newLinkedHashMap(size);
    for (int i = 0; i < size; i++) {
      result.put(keyMapper.apply(this), valueMapper.apply(this));
    }
    return result;
  }

}
