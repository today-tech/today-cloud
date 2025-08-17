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
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see java.io.DataOutput
 * @since 1.0 2025/8/16 16:58
 */
public interface Output {

  /**
   * Writes to the output stream the eight
   * low-order bits of the argument {@code b}.
   * The 24 high-order  bits of {@code b}
   * are ignored.
   *
   * @param b the byte to be written.
   * @throws SerializationException if an I/O error occurs.
   */
  void write(byte b) throws SerializationException;

  /**
   * Writes to the output stream all the bytes in array {@code b}.
   * If {@code b} is {@code null},
   * a {@code NullPointerException} is thrown.
   * If {@code b.length} is zero, then
   * no bytes are written. Otherwise, the byte
   * {@code b[0]} is written first, then
   * {@code b[1]}, and so on; the last byte
   * written is {@code b[b.length-1]}.
   *
   * @param b the data.
   * @throws SerializationException if an I/O error occurs.
   */
  void write(byte[] b) throws SerializationException;

  /**
   * Writes {@code len} bytes from array
   * {@code b}, in order,  to
   * the output stream.  If {@code b}
   * is {@code null}, a {@code NullPointerException}
   * is thrown.  If {@code off} is negative,
   * or {@code len} is negative, or {@code off+len}
   * is greater than the length of the array
   * {@code b}, then an {@code IndexOutOfBoundsException}
   * is thrown.  If {@code len} is zero,
   * then no bytes are written. Otherwise, the
   * byte {@code b[off]} is written first,
   * then {@code b[off+1]}, and so on; the
   * last byte written is {@code b[off+len-1]}.
   *
   * @param b the data.
   * @param off the start offset in the data.
   * @param len the number of bytes to write.
   * @throws SerializationException if an I/O error occurs.
   */
  void write(byte[] b, int off, int len) throws SerializationException;

  /**
   * Writes a {@code boolean} value to this output stream.
   * If the argument {@code v}
   * is {@code true}, the value {@code (byte)1}
   * is written; if {@code v} is {@code false},
   * the  value {@code (byte)0} is written.
   * The byte written by this method may
   * be read by the {@code readBoolean}
   * method of interface {@code DataInput},
   * which will then return a {@code boolean}
   * equal to {@code v}.
   *
   * @param v the boolean to be written.
   * @throws SerializationException if an I/O error occurs.
   */
  void write(boolean v) throws SerializationException;

  /**
   * Writes two bytes to the output
   * stream to represent the value of the argument.
   * The byte values to be written, in the  order
   * shown, are:
   * <pre>{@code
   * (byte)(0xff & (v >> 8))
   * (byte)(0xff & v)
   * }</pre> <p>
   * The bytes written by this method may be
   * read by the {@code readShort} method
   * of interface {@code DataInput}, which
   * will then return a {@code short} equal
   * to {@code (short)v}.
   *
   * @param v the {@code short} value to be written.
   * @throws SerializationException if an I/O error occurs.
   */
  void write(short v) throws SerializationException;

  /**
   * Writes an {@code int} value, which is
   * comprised of four bytes, to the output stream.
   * The byte values to be written, in the  order
   * shown, are:
   * <pre>{@code
   * (byte)(0xff & (v >> 24))
   * (byte)(0xff & (v >> 16))
   * (byte)(0xff & (v >>  8))
   * (byte)(0xff & v)
   * }</pre><p>
   * The bytes written by this method may be read
   * by the {@code readInt} method of interface
   * {@code DataInput}, which will then
   * return an {@code int} equal to {@code v}.
   *
   * @param v the {@code int} value to be written.
   * @throws SerializationException if an I/O error occurs.
   */
  void write(int v) throws SerializationException;

  /**
   * Writes a {@code long} value, which is
   * comprised of eight bytes, to the output stream.
   * The byte values to be written, in the  order
   * shown, are:
   * <pre>{@code
   * (byte)(0xff & (v >> 56))
   * (byte)(0xff & (v >> 48))
   * (byte)(0xff & (v >> 40))
   * (byte)(0xff & (v >> 32))
   * (byte)(0xff & (v >> 24))
   * (byte)(0xff & (v >> 16))
   * (byte)(0xff & (v >>  8))
   * (byte)(0xff & v)
   * }</pre><p>
   * The bytes written by this method may be
   * read by the {@code readLong} method
   * of interface {@code DataInput}, which
   * will then return a {@code long} equal
   * to {@code v}.
   *
   * @param v the {@code long} value to be written.
   * @throws SerializationException if an I/O error occurs.
   */
  void write(long v) throws SerializationException;

  /**
   * Writes a {@code float} value,
   * which is comprised of four bytes, to the output stream.
   * It does this as if it first converts this
   * {@code float} value to an {@code int}
   * in exactly the manner of the {@code Float.floatToIntBits}
   * method  and then writes the {@code int}
   * value in exactly the manner of the  {@code writeInt}
   * method.  The bytes written by this method
   * may be read by the {@code readFloat}
   * method of interface {@code DataInput},
   * which will then return a {@code float}
   * equal to {@code v}.
   *
   * @param v the {@code float} value to be written.
   * @throws SerializationException if an I/O error occurs.
   */
  void write(float v) throws SerializationException;

  /**
   * Writes a {@code double} value,
   * which is comprised of eight bytes, to the output stream.
   * It does this as if it first converts this
   * {@code double} value to a {@code long}
   * in exactly the manner of the {@code Double.doubleToLongBits}
   * method  and then writes the {@code long}
   * value in exactly the manner of the  {@code writeLong}
   * method. The bytes written by this method
   * may be read by the {@code readDouble}
   * method of interface {@code DataInput},
   * which will then return a {@code double}
   * equal to {@code v}.
   *
   * @param v the {@code double} value to be written.
   * @throws SerializationException if an I/O error occurs.
   */
  void write(double v) throws SerializationException;

  /**
   * Writes string
   *
   * @param s the string value to be written.
   * @throws SerializationException if an I/O error occurs.
   */
  void write(String s) throws SerializationException;

  default void write(Message message) throws SerializationException {
    message.writeTo(this);
  }

}
