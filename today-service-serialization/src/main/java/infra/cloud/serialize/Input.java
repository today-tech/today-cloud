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

/**
 * An Input lets an application read primitive data types and objects from a source of data.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see java.io.DataInput
 * @since 1.0 2025/8/16 16:59
 */
public interface Input {

  /**
   * Reads some bytes from an input
   * stream and stores them into the buffer
   * array {@code b}. The number of bytes
   * read is equal
   * to the length of {@code b}.
   *
   * @param b the buffer into which the data is read.
   * @throws NullPointerException if {@code b} is {@code null}.
   */
  void read(byte[] b) throws SerializationException;

  /**
   * Reads {@code len} bytes from an input.
   *
   * @param b the buffer into which the data is read.
   * @param off an int specifying the offset in the data array {@code b}.
   * @param len an int specifying the number of bytes to read.
   * @throws NullPointerException if {@code b} is {@code null}.
   * @throws IndexOutOfBoundsException if {@code off} is negative,
   * {@code len} is negative, or {@code len} is greater than
   * {@code b.length - off}.
   */
  void read(byte[] b, int off, int len) throws SerializationException;

  byte[] read();

  default void read(Message message) throws SerializationException {
    message.readFrom(this);
  }

  /**
   * Makes an attempt to skip over
   * {@code n} bytes
   * of data from the input
   * stream, discarding the skipped bytes. However,
   * it may skip
   * over some smaller number of
   * bytes, possibly zero. This may result from
   * any of a
   * number of conditions; reaching
   * end of file before {@code n} bytes
   * have been skipped is
   * only one possibility.
   * This method never throws an {@code EOFException}.
   * The actual
   * number of bytes skipped is returned.
   *
   * @param n the number of bytes to be skipped.
   * @return the number of bytes actually skipped.
   * @throws SerializationException if an I/O error occurs.
   */
  int skipBytes(int n) throws SerializationException;

  /**
   * Reads one input byte and returns
   * {@code true} if that byte is nonzero,
   * {@code false} if that byte is zero.
   * This method is suitable for reading
   * the byte written by the {@code writeBoolean}
   * method of interface {@code Input}.
   *
   * @return the {@code boolean} value read.
   * @throws SerializationException if an I/O error occurs.
   */
  boolean readBoolean() throws SerializationException;

  /**
   * Reads and returns one input byte.
   * The byte is treated as a signed value in
   * the range {@code -128} through {@code 127},
   * inclusive.
   * This method is suitable for
   * reading the byte written by the {@code writeByte}
   * method of interface {@code Input}.
   *
   * @return the 8-bit value read.
   * @throws SerializationException if an I/O error occurs.
   */
  byte readByte() throws SerializationException;

  /**
   * Reads one input byte, zero-extends
   * it to type {@code int}, and returns
   * the result, which is therefore in the range
   * {@code 0}
   * through {@code 255}.
   * This method is suitable for reading
   * the byte written by the {@code writeByte}
   * method of interface {@code Input}
   * if the argument to {@code writeByte}
   * was intended to be a value in the range
   * {@code 0} through {@code 255}.
   *
   * @return the unsigned 8-bit value read.
   * @throws SerializationException if an I/O error occurs.
   */
  int readUnsignedByte() throws SerializationException;

  /**
   * Reads two input bytes and returns
   * a {@code short} value. Let {@code a}
   * be the first byte read and {@code b}
   * be the second byte. The value
   * returned
   * is:
   * <pre>{@code (short)((a << 8) | (b & 0xff))
   * }</pre>
   * This method
   * is suitable for reading the bytes written
   * by the {@code writeShort} method of
   * interface {@code Input}.
   *
   * @return the 16-bit value read.
   * @throws SerializationException if an I/O error occurs.
   */
  short readShort() throws SerializationException;

  /**
   * Reads two input bytes and returns
   * an {@code int} value in the range {@code 0}
   * through {@code 65535}. Let {@code a}
   * be the first byte read and
   * {@code b}
   * be the second byte. The value returned is:
   * <pre>{@code (((a & 0xff) << 8) | (b & 0xff))
   * }</pre>
   * This method is suitable for reading the bytes
   * written by the {@code writeShort} method
   * of interface {@code Input}  if
   * the argument to {@code writeShort}
   * was intended to be a value in the range
   * {@code 0} through {@code 65535}.
   *
   * @return the unsigned 16-bit value read.
   * @throws SerializationException if an I/O error occurs.
   */
  int readUnsignedShort() throws SerializationException;

  /**
   * Reads two input bytes and returns a {@code char} value.
   * Let {@code a}
   * be the first byte read and {@code b}
   * be the second byte. The value
   * returned is:
   * <pre>{@code (char)((a << 8) | (b & 0xff))
   * }</pre>
   * This method
   * is suitable for reading bytes written by
   * the {@code writeChar} method of interface
   * {@code Input}.
   *
   * @return the {@code char} value read.
   * @throws SerializationException if an I/O error occurs.
   */
  char readChar() throws SerializationException;

  /**
   * Reads four input bytes and returns an
   * {@code int} value. Let {@code a-d}
   * be the first through fourth bytes read. The value returned is:
   * <pre>{@code
   * (((a & 0xff) << 24) | ((b & 0xff) << 16) |
   *  ((c & 0xff) <<  8) | (d & 0xff))
   * }</pre>
   * This method is suitable
   * for reading bytes written by the {@code writeInt}
   * method of interface {@code Input}.
   *
   * @return the {@code int} value read.
   * @throws SerializationException if an I/O error occurs.
   */
  int readInt() throws SerializationException;

  /**
   * Reads eight input bytes and returns
   * a {@code long} value. Let {@code a-h}
   * be the first through eighth bytes read.
   * The value returned is:
   * <pre>{@code
   * (((long)(a & 0xff) << 56) |
   *  ((long)(b & 0xff) << 48) |
   *  ((long)(c & 0xff) << 40) |
   *  ((long)(d & 0xff) << 32) |
   *  ((long)(e & 0xff) << 24) |
   *  ((long)(f & 0xff) << 16) |
   *  ((long)(g & 0xff) <<  8) |
   *  ((long)(h & 0xff)))
   * }</pre>
   * <p>
   * This method is suitable
   * for reading bytes written by the {@code writeLong}
   * method of interface {@code Input}.
   *
   * @return the {@code long} value read.
   * @throws SerializationException if an I/O error occurs.
   */
  long readLong() throws SerializationException;

  /**
   * Reads four input bytes and returns
   * a {@code float} value. It does this
   * by first constructing an {@code int}
   * value in exactly the manner
   * of the {@code readInt}
   * method, then converting this {@code int}
   * value to a {@code float} in
   * exactly the manner of the method {@code Float.intBitsToFloat}.
   * This method is suitable for reading
   * bytes written by the {@code writeFloat}
   * method of interface {@code Input}.
   *
   * @return the {@code float} value read.
   * @throws SerializationException if an I/O error occurs.
   */
  float readFloat() throws SerializationException;

  /**
   * Reads eight input bytes and returns
   * a {@code double} value. It does this
   * by first constructing a {@code long}
   * value in exactly the manner
   * of the {@code readLong}
   * method, then converting this {@code long}
   * value to a {@code double} in exactly
   * the manner of the method {@code Double.longBitsToDouble}.
   * This method is suitable for reading
   * bytes written by the {@code writeDouble}
   * method of interface {@code Input}.
   *
   * @return the {@code double} value read.
   * @throws SerializationException if an I/O error occurs.
   */
  double readDouble() throws SerializationException;

  /**
   * Reads a {@link String} field value.
   *
   * @return a string.
   * @throws SerializationException if an I/O error occurs.
   */
  String readString() throws SerializationException;

}
