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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import infra.lang.Nullable;
import io.netty.buffer.ByteBuf;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/12/20 21:28
 */
public class DefaultByteBufOutput implements Output {

  private final ByteBuf buffer;

  public DefaultByteBufOutput(ByteBuf buffer) {
    this.buffer = buffer;
  }

  @Override
  public void write(byte b) {
    buffer.writeByte(b);
  }

  @Override
  public void write(@Nullable byte[] b) {
    if (b == null || b.length == 0) {
      buffer.writeInt(0);
    }
    else {
      buffer.writeInt(b.length);
      buffer.writeBytes(b);
    }
  }

  @Override
  public void write(byte[] b, int off, int len) {
    buffer.writeInt(len);
    buffer.writeBytes(b, off, len);
  }

  @Override
  public void writeFully(byte[] b) {
    buffer.writeBytes(b);
  }

  @Override
  public void writeFully(byte[] b, int off, int len) {
    buffer.writeBytes(b, off, len);
  }

  @Override
  public void write(boolean v) {
    buffer.writeBoolean(v);
  }

  @Override
  public void write(short v) {
    buffer.writeShort(v);
  }

  @Override
  public void write(int v) {
    buffer.writeInt(v);
  }

  @Override
  public void write(long v) {
    buffer.writeLong(v);
  }

  @Override
  public void write(float v) {
    buffer.writeFloat(v);
  }

  @Override
  public void write(double v) {
    buffer.writeDouble(v);
  }

  @Override
  public void write(String s) {
    int length = s.length();
    buffer.writeShort(length);
    buffer.writeCharSequence(s, StandardCharsets.UTF_8);
  }

  @Override
  public void writeTimestamp(long millis) {
    write(Instant.ofEpochMilli(millis));
  }

  @Override
  public void write(Instant instant) {
    writeTimestamp(instant.getEpochSecond(), instant.getNano());
  }

  @Override
  public void writeTimestamp(long epochSecond, int nanoAdjustment) throws ArithmeticException {
    buffer.writeLong(epochSecond);
    buffer.writeInt(nanoAdjustment);
  }

  @Override
  public void write(Message message) {
    message.writeTo(this);
  }

  @Override
  public <T> void write(List<T> list, Consumer<T> mapper) {
    int size = list.size();
    buffer.writeInt(size);
    for (T t : list) {
      mapper.accept(t);
    }
  }

  @Override
  public <T> void write(List<T> list, BiConsumer<Output, T> mapper) {
    int size = list.size();
    buffer.writeInt(size);
    for (T t : list) {
      mapper.accept(this, t);
    }
  }

  @Override
  public <K, V> void write(Map<K, V> map, BiConsumer<Output, K> keyMapper, BiConsumer<Output, V> valueMapper) {
    int size = map.size();
    buffer.writeInt(size);
    for (Map.Entry<K, V> entry : map.entrySet()) {
      keyMapper.accept(this, entry.getKey());
      valueMapper.accept(this, entry.getValue());
    }
  }

  @Override
  public <K, V> void write(Map<K, V> map, Consumer<K> keyMapper, Consumer<V> valueMapper) {
    int size = map.size();
    buffer.writeInt(size);
    for (Map.Entry<K, V> entry : map.entrySet()) {
      keyMapper.accept(entry.getKey());
      valueMapper.accept(entry.getValue());
    }
  }
}
