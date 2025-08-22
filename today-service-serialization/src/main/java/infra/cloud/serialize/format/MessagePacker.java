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

package infra.cloud.serialize.format;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.time.Instant;
import java.util.Objects;

import infra.cloud.serialize.format.buffer.ChannelBufferOutput;
import infra.cloud.serialize.format.buffer.MessageBuffer;
import infra.cloud.serialize.format.buffer.MessageBufferOutput;
import infra.cloud.serialize.format.buffer.OutputStreamBufferOutput;
import infra.cloud.serialize.format.value.Value;
import infra.lang.Nullable;

import static infra.cloud.serialize.format.MessagePack.Code.ARRAY16;
import static infra.cloud.serialize.format.MessagePack.Code.ARRAY32;
import static infra.cloud.serialize.format.MessagePack.Code.BIN16;
import static infra.cloud.serialize.format.MessagePack.Code.BIN32;
import static infra.cloud.serialize.format.MessagePack.Code.BIN8;
import static infra.cloud.serialize.format.MessagePack.Code.EXT16;
import static infra.cloud.serialize.format.MessagePack.Code.EXT32;
import static infra.cloud.serialize.format.MessagePack.Code.EXT8;
import static infra.cloud.serialize.format.MessagePack.Code.EXT_TIMESTAMP;
import static infra.cloud.serialize.format.MessagePack.Code.FALSE;
import static infra.cloud.serialize.format.MessagePack.Code.FIXARRAY_PREFIX;
import static infra.cloud.serialize.format.MessagePack.Code.FIXEXT1;
import static infra.cloud.serialize.format.MessagePack.Code.FIXEXT16;
import static infra.cloud.serialize.format.MessagePack.Code.FIXEXT2;
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
 * MessagePack serializer that converts objects into binary.
 * You can use factory methods of {@link MessagePack} class or {@link MessagePack.PackerConfig} class to create
 * an instance.
 * <p>
 * This class provides following primitive methods to write MessagePack values. These primitive methods write
 * short bytes (1 to 7 bytes) to the internal buffer at once. There are also some utility methods for convenience.
 * <p>
 * Primitive methods:
 *
 * <table>
 *   <tr><th>Java type</th><th>Packer method</th><th>MessagePack type</th></tr>
 *   <tr><td>null</td><td>{@link #packNil()}</td><td>Nil</td></tr>
 *   <tr><td>boolean</td><td>{@link #packBoolean(boolean)}</td><td>Boolean</td></tr>
 *   <tr><td>byte</td><td>{@link #packByte(byte)}</td><td>Integer</td></tr>
 *   <tr><td>short</td><td>{@link #packShort(short)}</td><td>Integer</td></tr>
 *   <tr><td>int</td><td>{@link #packInt(int)}</td><td>Integer</td></tr>
 *   <tr><td>long</td><td>{@link #packLong(long)}</td><td>Integer</td></tr>
 *   <tr><td>BigInteger</td><td>{@link #packBigInteger(BigInteger)}</td><td>Integer</td></tr>
 *   <tr><td>float</td><td>{@link #packFloat(float)}</td><td>Float</td></tr>
 *   <tr><td>double</td><td>{@link #packDouble(double)}</td><td>Float</td></tr>
 *   <tr><td>byte[]</td><td>{@link #packBinaryHeader(int)}</td><td>Binary</td></tr>
 *   <tr><td>String</td><td>{@link #packRawStringHeader(int)}</td><td>String</td></tr>
 *   <tr><td>List</td><td>{@link #packArrayHeader(int)}</td><td>Array</td></tr>
 *   <tr><td>Map</td><td>{@link #packMapHeader(int)}</td><td>Map</td></tr>
 *   <tr><td>custom user type</td><td>{@link #packExtensionTypeHeader(byte, int)}</td><td>Extension</td></tr>
 * </table>
 *
 * <p>
 * Utility methods:
 *
 * <table>
 *   <tr><th>Java type</th><th>Packer method</th><th>MessagePack type</th></tr>
 *   <tr><td>String</td><td>{@link #packString(String)}</td><td>String</td></tr>
 *   <tr><td>{@link Value}</td><td>{@link #packValue(Value)}</td><td></td></tr>
 * </table>
 *
 * <p>
 * To write a byte array, first you call {@link #packBinaryHeader} method with length of the byte array. Then,
 * you call {@link #writePayload(byte[], int, int)} or {@link #addPayload(byte[], int, int)} method to write the
 * contents.
 *
 * <p>
 * To write a List, Collection or array, first you call {@link #packArrayHeader(int)} method with the number of
 * elements. Then, you call packer methods for each element.
 * iteration.
 *
 * <p>
 * To write a Map, first you call {@link #packMapHeader(int)} method with size of the map. Then, for each pair,
 * you call packer methods for key first, and then value. You will call packer methods twice as many time as the
 * size of the map.
 *
 * <p>
 * Note that packXxxHeader methods don't validate number of elements. You must call packer methods for correct
 * number of times to produce valid MessagePack data.
 *
 * <p>
 * When IOException is thrown, primitive methods guarantee that all data is written to the internal buffer or no data
 * is written. This is convenient behavior when you use a non-blocking output channel that may not be writable
 * immediately.
 */
public class MessagePacker implements Closeable, Flushable {

  private final int smallStringOptimizationThreshold;

  private final int bufferFlushThreshold;

  private final boolean str8FormatSupport;

  /**
   * Current internal buffer.
   */
  protected MessageBufferOutput out;

  @Nullable
  private MessageBuffer buffer;

  private int position;

  /**
   * Total written byte size
   */
  private long totalFlushBytes;

  /**
   * String encoder
   */
  @Nullable
  private CharsetEncoder encoder;

  /**
   * Create an MessagePacker that outputs the packed data to the given {@link MessageBufferOutput}.
   * This method is available for subclasses to override. Use MessagePack.PackerConfig.newPacker method to instantiate this implementation.
   *
   * @param out MessageBufferOutput. Use {@link OutputStreamBufferOutput}, {@link ChannelBufferOutput} or
   * your own implementation of {@link MessageBufferOutput} interface.
   */
  protected MessagePacker(MessageBufferOutput out, MessagePack.PackerConfig config) {
    this.out = Objects.requireNonNull(out, "MessageBufferOutput is null");
    this.smallStringOptimizationThreshold = config.getSmallStringOptimizationThreshold();
    this.bufferFlushThreshold = config.getBufferFlushThreshold();
    this.str8FormatSupport = config.isStr8FormatSupport();
    this.position = 0;
    this.totalFlushBytes = 0;
  }

  /**
   * Replaces underlying output.
   * <p>
   * This method flushes current internal buffer to the output, swaps it with the new given output, then returns
   * the old output.
   *
   * <p>
   * This method doesn't close the old output.
   *
   * @param out new output
   * @return the old output
   * @throws IOException when underlying output throws IOException
   * @throws NullPointerException the given output is null
   */
  public MessageBufferOutput reset(MessageBufferOutput out) throws IOException {
    // Validate the argument
    MessageBufferOutput newOut = Objects.requireNonNull(out, "MessageBufferOutput is null");

    // Flush before reset
    flush();
    MessageBufferOutput old = this.out;
    this.out = newOut;

    // Reset totalFlushBytes
    this.totalFlushBytes = 0;

    return old;
  }

  /**
   * Returns total number of written bytes.
   * <p>
   * This method returns total of amount of data flushed to the underlying output plus size of current
   * internal buffer.
   *
   * <p>
   * Calling {@link #reset(MessageBufferOutput)} resets this number to 0.
   */
  public long getTotalWrittenBytes() {
    return totalFlushBytes + position;
  }

  /**
   * Clears the written data.
   */
  public void clear() {
    position = 0;
  }

  /**
   * Flushes internal buffer to the underlying output.
   * <p>
   * This method also calls flush method of the underlying output after writing internal buffer.
   */
  @Override
  public void flush() throws IOException {
    if (position > 0) {
      flushBuffer();
    }
    out.flush();
  }

  /**
   * Closes underlying output.
   * <p>
   * This method flushes internal buffer before closing.
   */
  @Override
  public void close() throws IOException {
    try {
      flush();
    }
    finally {
      out.close();
    }
  }

  private void flushBuffer() throws IOException {
    out.writeBuffer(position);
    buffer = null;
    totalFlushBytes += position;
    position = 0;
  }

  private MessageBuffer ensureCapacity(int minimumSize) throws IOException {
    if (buffer == null) {
      buffer = out.next(minimumSize);
    }
    else if (position + minimumSize >= buffer.size()) {
      flushBuffer();
      buffer = out.next(minimumSize);
    }
    return buffer;
  }

  private void writeByte(byte b) throws IOException {
    ensureCapacity(1).putByte(position++, b);
  }

  private void writeByteAndByte(byte b, byte v) throws IOException {
    MessageBuffer buffer = ensureCapacity(2);
    buffer.putByte(position++, b);
    buffer.putByte(position++, v);
  }

  private void writeByteAndShort(byte b, short v) throws IOException {
    MessageBuffer buffer = ensureCapacity(3);
    buffer.putByte(position++, b);
    buffer.putShort(position, v);
    position += 2;
  }

  private void writeByteAndInt(byte b, int v) throws IOException {
    MessageBuffer buffer = ensureCapacity(5);
    buffer.putByte(position++, b);
    buffer.putInt(position, v);
    position += 4;
  }

  private void writeByteAndFloat(byte b, float v) throws IOException {
    MessageBuffer buffer = ensureCapacity(5);
    buffer.putByte(position++, b);
    buffer.putFloat(position, v);
    position += 4;
  }

  private void writeByteAndDouble(byte b, double v) throws IOException {
    MessageBuffer buffer = ensureCapacity(9);
    buffer.putByte(position++, b);
    buffer.putDouble(position, v);
    position += 8;
  }

  private void writeByteAndLong(byte b, long v) throws IOException {
    MessageBuffer buffer = ensureCapacity(9);
    buffer.putByte(position++, b);
    buffer.putLong(position, v);
    position += 8;
  }

  /**
   * Writes a Nil value.
   * <p>
   * This method writes a nil byte.
   *
   * @return this
   * @throws IOException when underlying output throws IOException
   */
  public MessagePacker packNil() throws IOException {
    writeByte(NIL);
    return this;
  }

  /**
   * Writes a Boolean value.
   * <p>
   * This method writes a true byte or a false byte.
   *
   * @return this
   * @throws IOException when underlying output throws IOException
   */
  public MessagePacker packBoolean(boolean b) throws IOException {
    writeByte(b ? TRUE : FALSE);
    return this;
  }

  /**
   * Writes an Integer value.
   *
   * <p>
   * This method writes an integer using the smallest format from the int format family.
   *
   * @param b the integer to be written
   * @return this
   * @throws IOException when underlying output throws IOException
   */
  public MessagePacker packByte(byte b) throws IOException {
    if (b < -(1 << 5)) {
      writeByteAndByte(INT8, b);
    }
    else {
      writeByte(b);
    }
    return this;
  }

  /**
   * Writes an Integer value.
   *
   * <p>
   * This method writes an integer using the smallest format from the int format family.
   *
   * @param v the integer to be written
   * @return this
   * @throws IOException when underlying output throws IOException
   */
  public MessagePacker packShort(short v) throws IOException {
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
    return this;
  }

  /**
   * Writes an Integer value.
   *
   * <p>
   * This method writes an integer using the smallest format from the int format family.
   *
   * @param r the integer to be written
   * @return this
   * @throws IOException when underlying output throws IOException
   */
  public MessagePacker packInt(int r) throws IOException {
    if (r < -(1 << 5)) {
      if (r < -(1 << 15)) {
        writeByteAndInt(INT32, r);
      }
      else if (r < -(1 << 7)) {
        writeByteAndShort(INT16, (short) r);
      }
      else {
        writeByteAndByte(INT8, (byte) r);
      }
    }
    else if (r < (1 << 7)) {
      writeByte((byte) r);
    }
    else {
      if (r < (1 << 8)) {
        writeByteAndByte(UINT8, (byte) r);
      }
      else if (r < (1 << 16)) {
        writeByteAndShort(UINT16, (short) r);
      }
      else {
        // unsigned 32
        writeByteAndInt(UINT32, r);
      }
    }
    return this;
  }

  /**
   * Writes an Integer value.
   *
   * <p>
   * This method writes an integer using the smallest format from the int format family.
   *
   * @param v the integer to be written
   * @return this
   * @throws IOException when underlying output throws IOException
   */
  public MessagePacker packLong(long v) throws IOException {
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
    return this;
  }

  /**
   * Writes an Integer value.
   *
   * <p>
   * This method writes an integer using the smallest format from the int format family.
   *
   * @param bi the integer to be written
   * @return this
   * @throws IOException when underlying output throws IOException
   */
  public MessagePacker packBigInteger(BigInteger bi) throws IOException {
    if (bi.bitLength() <= 63) {
      packLong(bi.longValue());
    }
    else if (bi.bitLength() == 64 && bi.signum() == 1) {
      writeByteAndLong(UINT64, bi.longValue());
    }
    else {
      throw new IllegalArgumentException("MessagePack cannot serialize BigInteger larger than 2^64-1");
    }
    return this;
  }

  /**
   * Writes a Float value.
   *
   * <p>
   * This method writes a float value using float format family.
   *
   * @param v the value to be written
   * @return this
   * @throws IOException when underlying output throws IOException
   */
  public MessagePacker packFloat(float v) throws IOException {
    writeByteAndFloat(FLOAT32, v);
    return this;
  }

  /**
   * Writes a Float value.
   *
   * <p>
   * This method writes a float value using float format family.
   *
   * @param v the value to be written
   * @return this
   * @throws IOException when underlying output throws IOException
   */
  public MessagePacker packDouble(double v) throws IOException {
    writeByteAndDouble(FLOAT64, v);
    return this;
  }

  private void packStringWithGetBytes(String s) throws IOException {
    // JVM performs various optimizations (memory allocation, reusing encoder etc.) when String.getBytes is used
    byte[] bytes = s.getBytes(MessagePack.UTF8);
    // Write the length and payload of small string to the buffer so that it avoids an extra flush of buffer
    packRawStringHeader(bytes.length);
    addPayload(bytes);
  }

  private void prepareEncoder() {
    if (encoder == null) {
      this.encoder = MessagePack.UTF8.newEncoder()
              .onMalformedInput(CodingErrorAction.REPLACE)
              .onUnmappableCharacter(CodingErrorAction.REPLACE);
    }
    encoder.reset();
  }

  private int encodeStringToBufferAt(MessageBuffer buffer, int pos, String s) {
    prepareEncoder();
    ByteBuffer bb = buffer.sliceAsByteBuffer(pos, buffer.size() - pos);
    int startPosition = bb.position();
    CharBuffer in = CharBuffer.wrap(s);
    CoderResult cr = encoder.encode(in, bb, true);
    if (cr.isError()) {
      try {
        cr.throwException();
      }
      catch (CharacterCodingException e) {
        throw new MessageStringCodingException(e);
      }
    }
    if (!cr.isUnderflow() || cr.isOverflow()) {
      // Underflow should be on to ensure all of the input string is encoded
      return -1;
    }
    // NOTE: This flush method does nothing if we use UTF8 encoder, but other general encoders require this
    cr = encoder.flush(bb);
    if (!cr.isUnderflow()) {
      return -1;
    }
    return bb.position() - startPosition;
  }

  private static final int UTF_8_MAX_CHAR_SIZE = 6;

  /**
   * Writes a String vlaue in UTF-8 encoding.
   *
   * <p>
   * This method writes a UTF-8 string using the smallest format from the str format family by default. If {@link MessagePack.PackerConfig#withStr8FormatSupport(boolean)} is set to false, smallest format from the str format family excepting str8 format.
   *
   * @param s the string to be written
   * @return this
   * @throws IOException when underlying output throws IOException
   */
  public MessagePacker packString(String s) throws IOException {
    if (s.isEmpty()) {
      packRawStringHeader(0);
      return this;
    }
    else if (s.length() < smallStringOptimizationThreshold) {
      // Using String.getBytes is generally faster for small strings.
      // Also, when running on a platform that has a corrupted CharsetEncoder (i.e. Android 4.x), avoid using it.
      packStringWithGetBytes(s);
      return this;
    }
    else if (s.length() < (1 << 8)) {
      // ensure capacity for 2-byte raw string header + the maximum string size (+ 1 byte for falback code)
      MessageBuffer buffer = ensureCapacity(2 + s.length() * UTF_8_MAX_CHAR_SIZE + 1);
      // keep 2-byte header region and write raw string
      int written = encodeStringToBufferAt(buffer, position + 2, s);
      if (written >= 0) {
        if (str8FormatSupport && written < (1 << 8)) {
          buffer.putByte(position++, STR8);
          buffer.putByte(position++, (byte) written);
          position += written;
        }
        else {
          if (written >= (1 << 16)) {
            // this must not happen because s.length() is less than 2^8 and (2^8) * UTF_8_MAX_CHAR_SIZE is less than 2^16
            throw new IllegalArgumentException("Unexpected UTF-8 encoder state");
          }
          // move 1 byte backward to expand 3-byte header region to 3 bytes
          buffer.putMessageBuffer(position + 3, buffer, position + 2, written);
          // write 3-byte header
          buffer.putByte(position++, STR16);
          buffer.putShort(position, (short) written);
          position += 2;
          position += written;
        }
        return this;
      }
    }
    else if (s.length() < (1 << 16)) {
      // ensure capacity for 3-byte raw string header + the maximum string size (+ 2 bytes for fallback code)
      MessageBuffer buffer = ensureCapacity(3 + s.length() * UTF_8_MAX_CHAR_SIZE + 2);
      // keep 3-byte header region and write raw string
      int written = encodeStringToBufferAt(buffer, position + 3, s);
      if (written >= 0) {
        if (written < (1 << 16)) {
          buffer.putByte(position++, STR16);
          buffer.putShort(position, (short) written);
          position += 2;
          position += written;
        }
        else {
          // move 2 bytes backward to expand 3-byte header region to 5 bytes
          buffer.putMessageBuffer(position + 5, buffer, position + 3, written);
          // write 3-byte header header
          buffer.putByte(position++, STR32);
          buffer.putInt(position, written);
          position += 4;
          position += written;
        }
        return this;
      }
    }

    // Here doesn't use above optimized code for s.length() < (1 << 32) so that
    // ensureCapacity is not called with an integer larger than (3 + ((1 << 16) * UTF_8_MAX_CHAR_SIZE) + 2).
    // This makes it sure that MessageBufferOutput.next won't be called a size larger than
    // 384KB, which is OK size to keep in memory.

    // fallback
    packStringWithGetBytes(s);
    return this;
  }

  /**
   * Writes a Timestamp value.
   *
   * <p>
   * This method writes a timestamp value using timestamp format family.
   *
   * @param instant the timestamp to be written
   * @return this packer
   * @throws IOException when underlying output throws IOException
   */
  public MessagePacker packTimestamp(Instant instant) throws IOException {
    return packTimestamp(instant.getEpochSecond(), instant.getNano());
  }

  /**
   * Writes a Timesamp value using a millisecond value (e.g., System.currentTimeMillis())
   *
   * @param millis the millisecond value
   * @return this packer
   * @throws IOException when underlying output throws IOException
   */
  public MessagePacker packTimestamp(long millis) throws IOException {
    return packTimestamp(Instant.ofEpochMilli(millis));
  }

  private static final long NANOS_PER_SECOND = 1000000000L;

  /**
   * Writes a Timestamp value.
   *
   * <p>
   * This method writes a timestamp value using timestamp format family.
   *
   * @param epochSecond the number of seconds from 1970-01-01T00:00:00Z
   * @param nanoAdjustment the nanosecond adjustment to the number of seconds, positive or negative
   * @return this
   * @throws IOException when underlying output throws IOException
   * @throws ArithmeticException when epochSecond plus nanoAdjustment in seconds exceeds the range of long
   */
  public MessagePacker packTimestamp(long epochSecond, int nanoAdjustment) throws IOException, ArithmeticException {
    long sec = Math.addExact(epochSecond, Math.floorDiv(nanoAdjustment, NANOS_PER_SECOND));
    long nsec = Math.floorMod((long) nanoAdjustment, NANOS_PER_SECOND);

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
    return this;
  }

  private void writeTimestamp32(int sec) throws IOException {
    // timestamp 32 in fixext 4
    MessageBuffer buffer = ensureCapacity(6);
    buffer.putByte(position++, FIXEXT4);
    buffer.putByte(position++, EXT_TIMESTAMP);
    buffer.putInt(position, sec);
    position += 4;
  }

  private void writeTimestamp64(long data64) throws IOException {
    // timestamp 64 in fixext 8
    MessageBuffer buffer = ensureCapacity(10);
    buffer.putByte(position++, FIXEXT8);
    buffer.putByte(position++, EXT_TIMESTAMP);
    buffer.putLong(position, data64);
    position += 8;
  }

  private void writeTimestamp96(long sec, int nsec) throws IOException {
    // timestamp 96 in ext 8
    MessageBuffer buffer = ensureCapacity(15);
    buffer.putByte(position++, EXT8);
    buffer.putByte(position++, (byte) 12);  // length of nsec and sec
    buffer.putByte(position++, EXT_TIMESTAMP);
    buffer.putInt(position, nsec);
    position += 4;
    buffer.putLong(position, sec);
    position += 8;
  }

  /**
   * Writes header of an Array value.
   * <p>
   * You will call other packer methods for each element after this method call.
   * <p>
   * You don't have to call anything at the end of iteration.
   *
   * @param arraySize number of elements to be written
   * @return this
   * @throws IOException when underlying output throws IOException
   */
  public MessagePacker packArrayHeader(int arraySize) throws IOException {
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
    return this;
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
   * @return this
   * @throws IOException when underlying output throws IOException
   */
  public MessagePacker packMapHeader(int mapSize) throws IOException {
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
    return this;
  }

  /**
   * Writes a dynamically typed value.
   *
   * @param v the value to be written
   * @return this
   * @throws IOException when underlying output throws IOException
   */
  public MessagePacker packValue(Value v) throws IOException {
    v.writeTo(this);
    return this;
  }

  /**
   * Writes header of an Extension value.
   * <p>
   * You MUST call {@link #writePayload(byte[])} or {@link #addPayload(byte[])} method to write body binary.
   *
   * @param extType the extension type tag to be written
   * @param payloadLen number of bytes of a payload binary to be written
   * @return this
   * @throws IOException when underlying output throws IOException
   */
  public MessagePacker packExtensionTypeHeader(byte extType, int payloadLen) throws IOException {
    if (payloadLen < (1 << 8)) {
      if (payloadLen > 0 && (payloadLen & (payloadLen - 1)) == 0) { // check whether dataLen == 2^x
        if (payloadLen == 1) {
          writeByteAndByte(FIXEXT1, extType);
        }
        else if (payloadLen == 2) {
          writeByteAndByte(FIXEXT2, extType);
        }
        else if (payloadLen == 4) {
          writeByteAndByte(FIXEXT4, extType);
        }
        else if (payloadLen == 8) {
          writeByteAndByte(FIXEXT8, extType);
        }
        else if (payloadLen == 16) {
          writeByteAndByte(FIXEXT16, extType);
        }
        else {
          writeByteAndByte(EXT8, (byte) payloadLen);
          writeByte(extType);
        }
      }
      else {
        writeByteAndByte(EXT8, (byte) payloadLen);
        writeByte(extType);
      }
    }
    else if (payloadLen < (1 << 16)) {
      writeByteAndShort(EXT16, (short) payloadLen);
      writeByte(extType);
    }
    else {
      writeByteAndInt(EXT32, payloadLen);
      writeByte(extType);

      // TODO support dataLen > 2^31 - 1
    }
    return this;
  }

  /**
   * Writes header of a Binary value.
   * <p>
   * You MUST call {@link #writePayload(byte[])} or {@link #addPayload(byte[])} method to write body binary.
   *
   * @param len number of bytes of a binary to be written
   * @return this
   * @throws IOException when underlying output throws IOException
   */
  public MessagePacker packBinaryHeader(int len) throws IOException {
    if (len < (1 << 8)) {
      writeByteAndByte(BIN8, (byte) len);
    }
    else if (len < (1 << 16)) {
      writeByteAndShort(BIN16, (short) len);
    }
    else {
      writeByteAndInt(BIN32, len);
    }
    return this;
  }

  /**
   * Writes header of a String value.
   * <p>
   * Length must be number of bytes of a string in UTF-8 encoding.
   * <p>
   * You MUST call {@link #writePayload(byte[])} or {@link #addPayload(byte[])} method to write body of the
   * UTF-8 encoded string.
   *
   * @param len number of bytes of a UTF-8 string to be written
   * @return this
   * @throws IOException when underlying output throws IOException
   */
  public MessagePacker packRawStringHeader(int len) throws IOException {
    if (len < (1 << 5)) {
      writeByte((byte) (FIXSTR_PREFIX | len));
    }
    else if (str8FormatSupport && len < (1 << 8)) {
      writeByteAndByte(STR8, (byte) len);
    }
    else if (len < (1 << 16)) {
      writeByteAndShort(STR16, (short) len);
    }
    else {
      writeByteAndInt(STR32, len);
    }
    return this;
  }

  /**
   * Writes a byte array to the output.
   * <p>
   * This method is used with {@link #packRawStringHeader(int)} or {@link #packBinaryHeader(int)} methods.
   * <p>
   * Unlike {@link #writePayload(byte[])} method, this method does not make a defensive copy of the given byte
   * array, even if it is shorter than {@link MessagePack.PackerConfig#withBufferFlushThreshold(int)}. This is
   * faster than {@link #writePayload(byte[])} method but caller must not modify the byte array after calling
   * this method.
   *
   * @param data the data to add
   * @return this
   * @throws IOException when underlying output throws IOException
   * @see #writePayload(byte[])
   */
  public MessagePacker packBinary(@Nullable byte[] data) throws IOException {
    if (data == null || data.length == 0) {
      return packBinaryHeader(0);
    }
    return packBinaryHeader(data.length)
            .addPayload(data);
  }

  /**
   * Writes a byte array to the output.
   * <p>
   * This method is used with {@link #packRawStringHeader(int)} or {@link #packBinaryHeader(int)} methods.
   *
   * @param src the data to add
   * @return this
   * @throws IOException when underlying output throws IOException
   */
  public MessagePacker writePayload(byte[] src) throws IOException {
    return writePayload(src, 0, src.length);
  }

  /**
   * Writes a byte array to the output.
   * <p>
   * This method is used with {@link #packRawStringHeader(int)} or {@link #packBinaryHeader(int)} methods.
   *
   * @param src the data to add
   * @param off the start offset in the data
   * @param len the number of bytes to add
   * @return this
   * @throws IOException when underlying output throws IOException
   */
  public MessagePacker writePayload(byte[] src, int off, int len) throws IOException {
    if (buffer == null || buffer.size() - position < len || len > bufferFlushThreshold) {
      flush();  // call flush before write
      // Directly write payload to the output without using the buffer
      out.write(src, off, len);
      totalFlushBytes += len;
    }
    else {
      buffer.putBytes(position, src, off, len);
      position += len;
    }
    return this;
  }

  /**
   * Writes a byte array to the output.
   * <p>
   * This method is used with {@link #packRawStringHeader(int)} or {@link #packBinaryHeader(int)} methods.
   * <p>
   * Unlike {@link #writePayload(byte[])} method, this method does not make a defensive copy of the given byte
   * array, even if it is shorter than {@link MessagePack.PackerConfig#withBufferFlushThreshold(int)}. This is
   * faster than {@link #writePayload(byte[])} method but caller must not modify the byte array after calling
   * this method.
   *
   * @param src the data to add
   * @return this
   * @throws IOException when underlying output throws IOException
   * @see #writePayload(byte[])
   */
  public MessagePacker addPayload(byte[] src) throws IOException {
    return addPayload(src, 0, src.length);
  }

  /**
   * Writes a byte array to the output.
   * <p>
   * This method is used with {@link #packRawStringHeader(int)} or {@link #packBinaryHeader(int)} methods.
   * <p>
   * Unlike {@link #writePayload(byte[], int, int)} method, this method does not make a defensive copy of the
   * given byte array, even if it is shorter than {@link MessagePack.PackerConfig#withBufferFlushThreshold(int)}.
   * This is faster than {@link #writePayload(byte[])} method but caller must not modify the byte array after
   * calling this method.
   *
   * @param src the data to add
   * @param off the start offset in the data
   * @param len the number of bytes to add
   * @return this
   * @throws IOException when underlying output throws IOException
   * @see #writePayload(byte[], int, int)
   */
  public MessagePacker addPayload(byte[] src, int off, int len) throws IOException {
    if (buffer == null || buffer.size() - position < len || len > bufferFlushThreshold) {
      flush();  // call flush before add
      // Directly add the payload without using the buffer
      out.add(src, off, len);
      totalFlushBytes += len;
    }
    else {
      buffer.putBytes(position, src, off, len);
      position += len;
    }
    return this;
  }

  //

  public MessagePacker write(boolean val) throws IOException {
    return packBoolean(val);
  }

  public MessagePacker write(byte val) throws IOException {
    return packByte(val);
  }

  public MessagePacker write(short val) throws IOException {
    return packShort(val);
  }

  public MessagePacker write(int val) throws IOException {
    return packInt(val);
  }

  public MessagePacker write(long val) throws IOException {
    return packLong(val);
  }

  public MessagePacker write(float val) throws IOException {
    return packFloat(val);
  }

  public MessagePacker write(double val) throws IOException {
    return packDouble(val);
  }

  public MessagePacker write(@Nullable byte[] bytes) throws IOException {
    return packBinary(bytes);
  }

  public MessagePacker write(String s) throws IOException {
    return packString(s);
  }

  public MessagePacker write(Instant instant) throws IOException {
    return packTimestamp(instant);
  }

}
