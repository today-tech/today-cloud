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

import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import infra.cloud.serialize.format.MessagePack;
import infra.lang.Nullable;
import io.netty.buffer.ByteBuf;

import static infra.cloud.serialize.format.MessagePack.Code.ARRAY16;
import static infra.cloud.serialize.format.MessagePack.Code.ARRAY32;
import static infra.cloud.serialize.format.MessagePack.Code.BIN16;
import static infra.cloud.serialize.format.MessagePack.Code.BIN32;
import static infra.cloud.serialize.format.MessagePack.Code.BIN8;
import static infra.cloud.serialize.format.MessagePack.Code.EXT8;
import static infra.cloud.serialize.format.MessagePack.Code.EXT_TIMESTAMP;
import static infra.cloud.serialize.format.MessagePack.Code.FALSE;
import static infra.cloud.serialize.format.MessagePack.Code.FIXARRAY_PREFIX;
import static infra.cloud.serialize.format.MessagePack.Code.FIXEXT4;
import static infra.cloud.serialize.format.MessagePack.Code.FIXEXT8;
import static infra.cloud.serialize.format.MessagePack.Code.FIXMAP_PREFIX;
import static infra.cloud.serialize.format.MessagePack.Code.FIXSTR_PREFIX;
import static infra.cloud.serialize.format.MessagePack.Code.FLOAT32;
import static infra.cloud.serialize.format.MessagePack.Code.FLOAT64;
import static infra.cloud.serialize.format.MessagePack.Code.INT16;
import static infra.cloud.serialize.format.MessagePack.Code.INT32;
import static infra.cloud.serialize.format.MessagePack.Code.INT64;
import static infra.cloud.serialize.format.MessagePack.Code.INT8;
import static infra.cloud.serialize.format.MessagePack.Code.MAP16;
import static infra.cloud.serialize.format.MessagePack.Code.MAP32;
import static infra.cloud.serialize.format.MessagePack.Code.NIL;
import static infra.cloud.serialize.format.MessagePack.Code.STR16;
import static infra.cloud.serialize.format.MessagePack.Code.STR32;
import static infra.cloud.serialize.format.MessagePack.Code.STR8;
import static infra.cloud.serialize.format.MessagePack.Code.TRUE;
import static infra.cloud.serialize.format.MessagePack.Code.UINT16;
import static infra.cloud.serialize.format.MessagePack.Code.UINT32;
import static infra.cloud.serialize.format.MessagePack.Code.UINT64;
import static infra.cloud.serialize.format.MessagePack.Code.UINT8;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/18 17:50
 */
public class MessagePackOutput implements Output {

  private static final long NANOS_PER_SECOND = 1000000000L;

  private final ByteBuf buffer;

  /**
   * String encoder
   */
  private CharsetEncoder encoder;

  public MessagePackOutput(ByteBuf buffer) {
    this.buffer = buffer;
  }

  /**
   * Writes a byte array to the output.
   * <p>
   * This method is used with {@link #writeStringHeader(int)} or {@link #writeBinaryHeader(int)} methods.
   * <p>
   * Unlike {@link #writeFully(byte[])} method, this method does not make a defensive copy of the given byte
   * array, even if it is shorter than {@link MessagePack.PackerConfig#withBufferFlushThreshold(int)}. This is
   * faster than {@link #writeFully(byte[])} method but caller must not modify the byte array after calling
   * this method.
   *
   * @param b the data to add
   * @throws SerializationException if an I/O error occurs.
   * @see #writeFully(byte[])
   */
  @Override
  public void write(@Nullable byte[] b) {
    if (b == null || b.length == 0) {
      writeBinaryHeader(0);
    }
    else {
      writeBinaryHeader(b.length);
      buffer.writeBytes(b);
    }
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
  public void write(byte[] b, int off, int len) {
    writeBinaryHeader(len);
    buffer.writeBytes(b, off, len);
  }

  public void writeNull() {
    writeByte(NIL);
  }

  @Override
  public void write(boolean v) {
    writeByte(v ? TRUE : FALSE);
  }

  @Override
  public void write(byte b) {
    if (b < -(1 << 5)) {
      writeByteAndByte(INT8, b);
    }
    else {
      writeByte(b);
    }
  }

  /**
   * Writes an Integer value.
   *
   * <p>
   * This method writes an integer using the smallest format from the int format family.
   *
   * @param v the integer to be written
   */
  @Override
  public void write(short v) {
    if (v < -(1 << 5)) {
      if (v < -(1 << 7)) {
        writeByteAndShort(INT16, v);
      }
      else {
        writeByteAndByte(INT8, (byte) v);
      }
    }
    else if (v < (1 << 7)) {
      writeByte((byte) v);
    }
    else {
      if (v < (1 << 8)) {
        writeByteAndByte(UINT8, (byte) v);
      }
      else {
        writeByteAndShort(UINT16, v);
      }
    }
  }

  /**
   * Writes an Integer value.
   *
   * <p>
   * This method writes an integer using the smallest format from the int format family.
   *
   * @param v the integer to be written
   */
  @Override
  public void write(int v) {
    if (v < -(1 << 5)) {
      if (v < -(1 << 15)) {
        writeByteAndInt(INT32, v);
      }
      else if (v < -(1 << 7)) {
        writeByteAndShort(INT16, (short) v);
      }
      else {
        writeByteAndByte(INT8, (byte) v);
      }
    }
    else if (v < (1 << 7)) {
      writeByte((byte) v);
    }
    else {
      if (v < (1 << 8)) {
        writeByteAndByte(UINT8, (byte) v);
      }
      else if (v < (1 << 16)) {
        writeByteAndShort(UINT16, (short) v);
      }
      else {
        // unsigned 32
        writeByteAndInt(UINT32, v);
      }
    }
  }

  /**
   * Writes an Integer value.
   *
   * <p>
   * This method writes an integer using the smallest format from the int format family.
   *
   * @param v the integer to be written
   */
  @Override
  public void write(long v) {
    if (v < -(1L << 5)) {
      if (v < -(1L << 15)) {
        if (v < -(1L << 31)) {
          writeByteAndLong(INT64, v);
        }
        else {
          writeByteAndInt(INT32, (int) v);
        }
      }
      else {
        if (v < -(1 << 7)) {
          writeByteAndShort(INT16, (short) v);
        }
        else {
          writeByteAndByte(INT8, (byte) v);
        }
      }
    }
    else if (v < (1 << 7)) {
      // fixnum
      writeByte((byte) v);
    }
    else {
      if (v < (1L << 16)) {
        if (v < (1 << 8)) {
          writeByteAndByte(UINT8, (byte) v);
        }
        else {
          writeByteAndShort(UINT16, (short) v);
        }
      }
      else {
        if (v < (1L << 32)) {
          writeByteAndInt(UINT32, (int) v);
        }
        else {
          writeByteAndLong(UINT64, v);
        }
      }
    }
  }

  @Override
  public void write(float v) {
    writeByteAndFloat(FLOAT32, v);
  }

  @Override
  public void write(double v) {
    writeByteAndDouble(FLOAT64, v);
  }

  @Override
  public void write(@Nullable String s) {
    if (s == null || s.isEmpty()) {
      writeStringHeader(0);
    }
    else {
      // JVM performs various optimizations (memory allocation, reusing encoder etc.) when String.getBytes is used
      byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
      writeStringHeader(bytes.length);
      writeFully(bytes);
    }
  }

  @Override
  public void write(Instant instant) {
    writeTimestamp(instant.getEpochSecond(), instant.getNano());
  }

  @Override
  public void writeTimestamp(long millis) {
    write(Instant.ofEpochMilli(millis));
  }

  @Override
  public void write(Message message) {
    message.writeTo(this);
  }

  @Override
  public void writeTimestamp(long epochSecond, int nanoAdjustment) {
    long sec = Math.addExact(epochSecond, Math.floorDiv(nanoAdjustment, NANOS_PER_SECOND));
    long nsec = Math.floorMod(nanoAdjustment, NANOS_PER_SECOND);

    if (sec >>> 34 == 0) {
      // sec can be serialized in 34 bits.
      long data64 = (nsec << 34) | sec;
      if ((data64 & 0xffffffff00000000L) == 0L) {
        // sec can be serialized in 32 bits and nsec is 0.
        // use timestamp 32
        writeTimestamp32((int) sec);
      }
      else {
        // sec exceeded 32 bits or nsec is not 0.
        // use timestamp 64
        writeTimestamp64(data64);
      }
    }
    else {
      // use timestamp 96 format
      writeTimestamp96(sec, (int) nsec);
    }
  }

  @Override
  public <T> void write(List<T> list, Consumer<T> mapper) {
    int size = list.size();
    writeArrayHeader(size);
    for (T t : list) {
      mapper.accept(t);
    }
  }

  @Override
  public <T> void write(List<T> list, BiConsumer<Output, T> mapper) {
    int size = list.size();
    writeArrayHeader(size);
    for (T t : list) {
      mapper.accept(this, t);
    }
  }

  @Override
  public <K, V> void write(Map<K, V> map, BiConsumer<Output, K> keyMapper, BiConsumer<Output, V> valueMapper) {
    int size = map.size();
    writeMapHeader(size);
    for (Map.Entry<K, V> entry : map.entrySet()) {
      keyMapper.accept(this, entry.getKey());
      valueMapper.accept(this, entry.getValue());
    }
  }

  @Override
  public <K, V> void write(Map<K, V> map, Consumer<K> keyMapper, Consumer<V> valueMapper) {
    int size = map.size();
    writeMapHeader(size);
    for (Map.Entry<K, V> entry : map.entrySet()) {
      keyMapper.accept(entry.getKey());
      valueMapper.accept(entry.getValue());
    }
  }

  /**
   * Writes header of a Binary value.
   * <p>
   * You MUST call {@link #writeFully(byte[])} method to write body binary.
   *
   * @param len number of bytes of a binary to be written
   */
  public void writeBinaryHeader(int len) {
    if (len < (1 << 8)) {
      writeByteAndByte(BIN8, (byte) len);
    }
    else if (len < (1 << 16)) {
      writeByteAndShort(BIN16, (short) len);
    }
    else {
      writeByteAndInt(BIN32, len);
    }
  }

  /**
   * Writes header of a String value.
   * <p>
   * Length must be number of bytes of a string in UTF-8 encoding.
   * <p>
   * You MUST call {@link #writeFully(byte[])} method to write body of the
   * UTF-8 encoded string.
   *
   * @param len number of bytes of a UTF-8 string to be written
   */
  public void writeStringHeader(int len) {
    if (len < (1 << 5)) {
      writeByte((byte) (FIXSTR_PREFIX | len));
    }
    else if (len < (1 << 8)) {
      writeByteAndByte(STR8, (byte) len);
    }
    else if (len < (1 << 16)) {
      writeByteAndShort(STR16, (short) len);
    }
    else {
      writeByteAndInt(STR32, len);
    }
  }

  /**
   * Writes header of an Array value.
   * <p>
   * You will call other packer methods for each element after this method call.
   * <p>
   * You don't have to call anything at the end of iteration.
   *
   * @param arraySize number of elements to be written
   */
  public void writeArrayHeader(int arraySize) {
    if (arraySize < 0) {
      throw new IllegalArgumentException("array size must be >= 0");
    }

    if (arraySize < (1 << 4)) {
      writeByte((byte) (FIXARRAY_PREFIX | arraySize));
    }
    else if (arraySize < (1 << 16)) {
      writeByteAndShort(ARRAY16, (short) arraySize);
    }
    else {
      writeByteAndInt(ARRAY32, arraySize);
    }
  }

  /**
   * Writes header of a Map value.
   * <p>
   * After this method call, for each key-value pair, you will call packer methods for key first, and then value.
   * You will call packer methods twice as many time as the size of the map.
   * <p>
   * You don't have to call anything at the end of iteration.
   *
   * @param mapSize number of pairs to be written
   */
  public void writeMapHeader(int mapSize) {
    if (mapSize < 0) {
      throw new IllegalArgumentException("map size must be >= 0");
    }

    if (mapSize < (1 << 4)) {
      writeByte((byte) (FIXMAP_PREFIX | mapSize));
    }
    else if (mapSize < (1 << 16)) {
      writeByteAndShort(MAP16, (short) mapSize);
    }
    else {
      writeByteAndInt(MAP32, mapSize);
    }
  }

  private void writeByte(byte b) {
    buffer.writeByte(b);
  }

  private void writeByteAndByte(byte b, byte v) {
    buffer.writeByte(b);
    buffer.writeByte(v);
  }

  private void writeByteAndShort(byte b, short v) {
    buffer.writeByte(b);
    buffer.writeShort(v);
  }

  private void writeByteAndInt(byte b, int v) {
    buffer.writeByte(b);
    buffer.writeInt(v);
  }

  private void writeByteAndFloat(byte b, float v) {
    buffer.writeByte(b);
    buffer.writeFloat(v);
  }

  private void writeByteAndDouble(byte b, double v) {
    buffer.writeByte(b);
    buffer.writeDouble(v);
  }

  private void writeByteAndLong(byte b, long v) {
    buffer.writeByte(b);
    buffer.writeLong(v);
  }

  private void writeTimestamp32(int sec) {
    // timestamp 32 in fixext 4
    buffer.writeByte(FIXEXT4);
    buffer.writeByte(EXT_TIMESTAMP);
    buffer.writeInt(sec);
  }

  private void writeTimestamp64(long data64) {
    // timestamp 64 in fixext 8
    buffer.writeByte(FIXEXT8);
    buffer.writeByte(EXT_TIMESTAMP);
    buffer.writeLong(data64);
  }

  private void writeTimestamp96(long sec, int nsec) {
    // timestamp 96 in ext 8
    buffer.writeByte(EXT8);
    buffer.writeByte((byte) 12);
    buffer.writeByte(EXT_TIMESTAMP);
    buffer.writeInt(nsec);
    buffer.writeLong(sec);
  }

  private void prepareEncoder() {
    if (encoder == null) {
      this.encoder = MessagePack.UTF8.newEncoder()
              .onMalformedInput(CodingErrorAction.REPLACE)
              .onUnmappableCharacter(CodingErrorAction.REPLACE);
    }
    encoder.reset();
  }

}
