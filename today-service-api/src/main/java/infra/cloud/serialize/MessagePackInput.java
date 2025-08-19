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

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import infra.cloud.serialize.format.ExtensionTypeHeader;
import infra.cloud.serialize.format.MessageFormat;
import infra.cloud.serialize.format.MessageFormatException;
import infra.cloud.serialize.format.MessageIntegerOverflowException;
import infra.cloud.serialize.format.MessageNeverUsedFormatException;
import infra.cloud.serialize.format.MessagePack.Code;
import infra.cloud.serialize.format.MessagePackException;
import infra.cloud.serialize.format.MessageSizeException;
import infra.cloud.serialize.format.MessageTypeException;
import infra.lang.Constant;
import infra.lang.TodayStrategies;
import infra.util.CollectionUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import static infra.cloud.serialize.format.MessagePack.Code.EXT_TIMESTAMP;

/**
 * Message pack specification input
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/18 14:52
 */
public class MessagePackInput implements Input {

  private static final int stringSizeLimit = TodayStrategies.getInt(
          "infra.cloud.serialize.stringSizeLimit", Integer.MAX_VALUE / 2);

  private static final Charset stringCharset = Charset.forName(TodayStrategies.getProperty(
          "infra.cloud.serialize.stringCharset", "UTF-8"));

  private final ByteBuf buffer;

  public MessagePackInput(ByteBuf buffer) {
    this.buffer = buffer;
  }

  @Override
  public byte[] readFully() {
    return ByteBufUtil.getBytes(buffer);
  }

  @Override
  public void read(byte[] b) {
    buffer.readBytes(b);
  }

  @Override
  public byte[] read() {
    return read(readBinaryHeader());
  }

  @Override
  public byte[] read(int len) {
    return ByteBufUtil.getBytes(buffer, buffer.readerIndex(), len);
  }

  @Override
  public void read(byte[] b, int off, int len) {
    buffer.readBytes(b, off, len);
  }

  @Override
  public void read(Message message) {
    message.readFrom(this);
  }

  @Override
  public int skipBytes(int n) {
    int start = buffer.readerIndex();
    buffer.skipBytes(n);
    return start - buffer.readerIndex();
  }

  @Override
  public boolean readBoolean() {
    byte b = buffer.readByte();
    if (b == Code.FALSE) {
      return false;
    }
    else if (b == Code.TRUE) {
      return true;
    }
    throw unexpected("boolean", b);
  }

  @Override
  public byte readByte() {
    byte b = readInt8();
    if (Code.isFixInt(b)) {
      return b;
    }
    switch (b) {
      case Code.UINT8: // unsigned int 8
        byte u8 = readInt8();
        if (u8 < (byte) 0) {
          throw overflowU8(u8);
        }
        return u8;
      case Code.UINT16: // unsigned int 16
        short u16 = readInt16();
        if (u16 < 0 || u16 > Byte.MAX_VALUE) {
          throw overflowU16(u16);
        }
        return (byte) u16;
      case Code.UINT32: // unsigned int 32
        int u32 = readInt32();
        if (u32 < 0 || u32 > Byte.MAX_VALUE) {
          throw overflowU32(u32);
        }
        return (byte) u32;
      case Code.UINT64: // unsigned int 64
        long u64 = readInt64();
        if (u64 < 0L || u64 > Byte.MAX_VALUE) {
          throw overflowU64(u64);
        }
        return (byte) u64;
      case Code.INT8: // signed int 8
        return readInt8();
      case Code.INT16: // signed int 16
        short i16 = readInt16();
        if (i16 < Byte.MIN_VALUE || i16 > Byte.MAX_VALUE) {
          throw overflowI16(i16);
        }
        return (byte) i16;
      case Code.INT32: // signed int 32
        int i32 = readInt32();
        if (i32 < Byte.MIN_VALUE || i32 > Byte.MAX_VALUE) {
          throw overflowI32(i32);
        }
        return (byte) i32;
      case Code.INT64: // signed int 64
        long i64 = readInt64();
        if (i64 < Byte.MIN_VALUE || i64 > Byte.MAX_VALUE) {
          throw overflowI64(i64);
        }
        return (byte) i64;
    }
    throw unexpected("Integer", b);
  }

  @Override
  public int readUnsignedByte() {
    return readByte() & 0xff;
  }

  @Override
  public short readShort() {
    byte b = readInt8();
    if (Code.isFixInt(b)) {
      return b;
    }
    switch (b) {
      case Code.UINT8: // unsigned int 8
        byte u8 = readInt8();
        return (short) (u8 & 0xff);
      case Code.UINT16: // unsigned int 16
        short u16 = readInt16();
        if (u16 < (short) 0) {
          throw overflowU16(u16);
        }
        return u16;
      case Code.UINT32: // unsigned int 32
        int u32 = readInt32();
        if (u32 < 0 || u32 > Short.MAX_VALUE) {
          throw overflowU32(u32);
        }
        return (short) u32;
      case Code.UINT64: // unsigned int 64
        long u64 = readInt64();
        if (u64 < 0L || u64 > Short.MAX_VALUE) {
          throw overflowU64(u64);
        }
        return (short) u64;
      case Code.INT8: // signed int 8
        return readInt8();
      case Code.INT16: // signed int 16
        return readInt16();
      case Code.INT32: // signed int 32
        int i32 = readInt32();
        if (i32 < Short.MIN_VALUE || i32 > Short.MAX_VALUE) {
          throw overflowI32(i32);
        }
        return (short) i32;
      case Code.INT64: // signed int 64
        long i64 = readInt64();
        if (i64 < Short.MIN_VALUE || i64 > Short.MAX_VALUE) {
          throw overflowI64(i64);
        }
        return (short) i64;
    }
    throw unexpected("Integer", b);
  }

  @Override
  public int readUnsignedShort() {
    return readShort() & 0xffff;
  }

  @Override
  public int readInt() {
    byte b = readInt8();
    if (Code.isFixInt(b)) {
      return b;
    }
    switch (b) {
      case Code.UINT8: // unsigned int 8
        byte u8 = readInt8();
        return u8 & 0xff;
      case Code.UINT16: // unsigned int 16
        short u16 = readInt16();
        return u16 & 0xffff;
      case Code.UINT32: // unsigned int 32
        int u32 = readInt32();
        if (u32 < 0) {
          throw overflowU32(u32);
        }
        return u32;
      case Code.UINT64: // unsigned int 64
        long u64 = readInt64();
        if (u64 < 0L || u64 > (long) Integer.MAX_VALUE) {
          throw overflowU64(u64);
        }
        return (int) u64;
      case Code.INT8: // signed int 8
        return readInt8();
      case Code.INT16: // signed int 16
        return readInt16();
      case Code.INT32: // signed int 32
        return readInt32();
      case Code.INT64: // signed int 64
        long i64 = readInt64();
        if (i64 < (long) Integer.MIN_VALUE || i64 > (long) Integer.MAX_VALUE) {
          throw overflowI64(i64);
        }
        return (int) i64;
    }
    throw unexpected("Integer", b);
  }

  @Override
  public long readLong() {
    byte b = readInt8();
    if (Code.isFixInt(b)) {
      return b;
    }
    switch (b) {
      case Code.UINT8: // unsigned int 8
        byte u8 = readInt8();
        return u8 & 0xff;
      case Code.UINT16: // unsigned int 16
        short u16 = readInt16();
        return u16 & 0xffff;
      case Code.UINT32: // unsigned int 32
        int u32 = readInt32();
        if (u32 < 0) {
          return (long) (u32 & 0x7fffffff) + 0x80000000L;
        }
        else {
          return u32;
        }
      case Code.UINT64: // unsigned int 64
        long u64 = readInt64();
        if (u64 < 0L) {
          throw overflowU64(u64);
        }
        return u64;
      case Code.INT8: // signed int 8
        return readInt8();
      case Code.INT16: // signed int 16
        return readInt16();
      case Code.INT32: // signed int 32
        return readInt32();
      case Code.INT64: // signed int 64
        return readInt64();
    }
    throw unexpected("Integer", b);
  }

  @Override
  public float readFloat() {
    byte b = readInt8();
    return switch (b) {
      case Code.FLOAT32 -> readFloat32();
      case Code.FLOAT64 -> (float) readFloat64();
      default -> throw unexpected("Float", b);
    };
  }

  @Override
  public double readDouble() {
    byte b = readInt8();
    return switch (b) {
      case Code.FLOAT32 -> readFloat32();
      case Code.FLOAT64 -> readFloat64();
      default -> throw unexpected("Float", b);
    };
  }

  @Override
  public String readString() {
    int len = readStringHeader();
    if (len == 0) {
      return Constant.BLANK;
    }
    if (len > stringSizeLimit) {
      throw new MessageSizeException("Cannot reading a String of size larger than %,d: %,d"
              .formatted(stringSizeLimit, len), len);
    }
    return buffer.readString(len, stringCharset);
  }

  @Override
  public Instant readTimestamp() {
    ExtensionTypeHeader ext = readExtensionTypeHeader();
    if (!ext.isTimestampType()) {
      throw unexpectedExtension("Timestamp", EXT_TIMESTAMP, ext.getType());
    }
    switch (ext.getLength()) {
      case 4: {
        // Need to convert Java's int (int32) to uint32
        long u32 = readInt32() & 0xffffffffL;
        return Instant.ofEpochSecond(u32);
      }
      case 8: {
        long data64 = readInt64();
        int nsec = (int) (data64 >>> 34);
        long sec = data64 & 0x00000003ffffffffL;
        return Instant.ofEpochSecond(sec, nsec);
      }
      case 12: {
        // Need to convert Java's int (int32) to uint32
        long nsecU32 = readInt32() & 0xffffffffL;
        long sec = readInt64();
        return Instant.ofEpochSecond(sec, nsecU32);
      }
      default:
        throw new MessageFormatException("Timestamp extension type (%d) expects 4, 8, or 12 bytes of payload but got %d bytes"
                .formatted(EXT_TIMESTAMP, ext.getLength()));
    }
  }

  @Override
  public <T> List<T> read(Function<Input, T> mapper) {
    int size = readArrayHeader();
    ArrayList<T> result = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      result.add(mapper.apply(this));
    }
    return result;
  }

  @Override
  public <T> List<T> read(Supplier<T> supplier) {
    int size = readArrayHeader();
    ArrayList<T> result = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      result.add(supplier.get());
    }
    return result;
  }

  @Override
  public <K, V> Map<K, V> read(Function<Input, K> keyMapper, Function<Input, V> valueMapper) {
    int size = readMapHeader();
    LinkedHashMap<K, V> result = CollectionUtils.newLinkedHashMap(size);
    for (int i = 0; i < size; i++) {
      result.put(keyMapper.apply(this), valueMapper.apply(this));
    }
    return result;
  }

  /**
   * Reads header of an array.
   *
   * <p>
   * This method returns number of elements to be read. After this method call, you call unpacker methods for
   * each element. You don't have to call anything at the end of iteration.
   *
   * @return the size of the array to be read
   * @throws MessageTypeException when value is not MessagePack Array type
   * @throws MessageSizeException when size of the array is larger than 2^31 - 1
   */
  public int readArrayHeader() {
    byte b = readInt8();
    if (Code.isFixedArray(b)) { // fixarray
      return b & 0x0f;
    }
    return switch (b) {
      // array 16
      case Code.ARRAY16 -> readNextLength16();
      // array 32
      case Code.ARRAY32 -> readNextLength32();
      default -> throw unexpected("Array", b);
    };
  }

  /**
   * Reads header of a map.
   *
   * <p>
   * This method returns number of pairs to be read. After this method call, for each pair, you call unpacker
   * methods for key first, and then value. You will call unpacker methods twice as many time as the returned
   * count. You don't have to call anything at the end of iteration.
   *
   * @return the size of the map to be read
   * @throws MessageTypeException when value is not MessagePack Map type
   * @throws MessageSizeException when size of the map is larger than 2^31 - 1
   */
  public int readMapHeader() {
    byte b = readInt8();
    if (Code.isFixedMap(b)) { // fixmap
      return b & 0x0f;
    }
    return switch (b) {
      case Code.MAP16 -> readNextLength16();// map 16
      case Code.MAP32 -> readNextLength32();// map 32
      default -> throw unexpected("Map", b);
    };
  }

  public ExtensionTypeHeader readExtensionTypeHeader() {
    byte b = readInt8();
    switch (b) {
      case Code.FIXEXT1: {
        byte type = readInt8();
        return new ExtensionTypeHeader(type, 1);
      }
      case Code.FIXEXT2: {
        byte type = readInt8();
        return new ExtensionTypeHeader(type, 2);
      }
      case Code.FIXEXT4: {
        byte type = readInt8();
        return new ExtensionTypeHeader(type, 4);
      }
      case Code.FIXEXT8: {
        byte type = readInt8();
        return new ExtensionTypeHeader(type, 8);
      }
      case Code.FIXEXT16: {
        byte type = readInt8();
        return new ExtensionTypeHeader(type, 16);
      }
      case Code.EXT8: {
        int length = readNextLength8();
        byte type = readInt8();
        return new ExtensionTypeHeader(type, length);
      }
      case Code.EXT16: {
        int length = readNextLength16();
        byte type = readInt8();
        return new ExtensionTypeHeader(type, length);
      }
      case Code.EXT32: {
        int length = readNextLength32();
        byte type = readInt8();
        return new ExtensionTypeHeader(type, length);
      }
    }

    throw unexpected("Ext", b);
  }

  /**
   * Read a byte value at the cursor and proceed the cursor.
   */
  private byte readInt8() {
    return buffer.readByte();
  }

  private short readInt16() {
    return buffer.readShort();
  }

  private int readInt32() {
    return buffer.readInt();
  }

  private long readInt64() {
    return buffer.readLong();
  }

  private float readFloat32() {
    return buffer.readFloat();
  }

  private double readFloat64() {
    return buffer.readDouble();
  }

  private int readNextLength8() {
    byte u8 = readInt8();
    return u8 & 0xff;
  }

  private int readNextLength16() {
    short u16 = readInt16();
    return u16 & 0xffff;
  }

  private int readNextLength32() {
    int u32 = readInt32();
    if (u32 < 0) {
      throw overflowU32Size(u32);
    }
    return u32;
  }

  private int tryReadStringHeader(byte b) {
    return switch (b) {
      case Code.STR8 -> readNextLength8(); // str 8
      case Code.STR16 -> readNextLength16(); // str 16
      case Code.STR32 -> readNextLength32(); // str 32
      default -> -1;
    };
  }

  private int tryReadBinaryHeader(byte b) {
    return switch (b) {
      case Code.BIN8 -> readNextLength8(); // bin 8
      case Code.BIN16 -> readNextLength16(); // bin 16
      case Code.BIN32 -> readNextLength32(); // bin 32
      default -> -1;
    };
  }

  public int readStringHeader() {
    byte b = readInt8();
    if (Code.isFixedRaw(b)) { // FixRaw
      return b & 0x1f;
    }
    int len = tryReadStringHeader(b);
    if (len >= 0) {
      return len;
    }

    throw unexpected("String", b);
  }

  /**
   * Reads header of a binary.
   *
   * <p>
   * This method returns number of bytes to be read. After this method call, you call a readPayload method such as
   * {@link #read(int)} with the returned count.
   *
   * <p>
   * You can divide readPayload method into multiple calls. In this case, you must repeat readPayload methods
   * until total amount of bytes becomes equal to the returned count.
   *
   * @return the size of the map to be read
   * @throws MessageTypeException when value is not MessagePack Map type
   * @throws MessageSizeException when size of the map is larger than 2^31 - 1
   * @ when underlying input
   */
  public int readBinaryHeader() {
    byte b = readInt8();
    if (Code.isFixedRaw(b)) { // FixRaw
      return b & 0x1f;
    }
    int len = tryReadBinaryHeader(b);
    if (len >= 0) {
      return len;
    }

    throw unexpected("Binary", b);
  }

  /**
   * Create an exception for the case when an unexpected byte value is read
   */
  private static MessagePackException unexpected(String expected, byte b) {
    MessageFormat format = MessageFormat.valueOf(b);
    if (format == MessageFormat.NEVER_USED) {
      return new MessageNeverUsedFormatException(String.format("Expected %s, but encountered 0xC1 \"NEVER_USED\" byte", expected));
    }
    else {
      String name = format.getValueType().name();
      String typeName = name.charAt(0) + name.substring(1).toLowerCase();
      return new MessageTypeException(String.format("Expected %s, but got %s (%02x)", expected, typeName, b));
    }
  }

  private static MessagePackException unexpectedExtension(String expected, int expectedType, int actualType) {
    return new MessageTypeException(String.format("Expected extension type %s (%d), but got extension type %d",
            expected, expectedType, actualType));
  }

  private static MessageIntegerOverflowException overflowU8(byte u8) {
    BigInteger bi = BigInteger.valueOf(u8 & 0xff);
    return new MessageIntegerOverflowException(bi);
  }

  private static MessageIntegerOverflowException overflowU16(short u16) {
    BigInteger bi = BigInteger.valueOf(u16 & 0xffff);
    return new MessageIntegerOverflowException(bi);
  }

  private static MessageIntegerOverflowException overflowU32(int u32) {
    BigInteger bi = BigInteger.valueOf((long) (u32 & 0x7fffffff) + 0x80000000L);
    return new MessageIntegerOverflowException(bi);
  }

  private static MessageIntegerOverflowException overflowU64(long u64) {
    BigInteger bi = BigInteger.valueOf(u64 + Long.MAX_VALUE + 1L).setBit(63);
    return new MessageIntegerOverflowException(bi);
  }

  private static MessageIntegerOverflowException overflowI16(short i16) {
    BigInteger bi = BigInteger.valueOf(i16);
    return new MessageIntegerOverflowException(bi);
  }

  private static MessageIntegerOverflowException overflowI32(int i32) {
    BigInteger bi = BigInteger.valueOf(i32);
    return new MessageIntegerOverflowException(bi);
  }

  private static MessageIntegerOverflowException overflowI64(long i64) {
    BigInteger bi = BigInteger.valueOf(i64);
    return new MessageIntegerOverflowException(bi);
  }

  private static MessageSizeException overflowU32Size(int u32) {
    long lv = (long) (u32 & 0x7fffffff) + 0x80000000L;
    return new MessageSizeException(lv);
  }

}
