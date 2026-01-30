/*
 * Copyright 2021 - 2026 the original author or authors.
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
 * Only the identity of the class of a Message instance is
 * written in the serialization stream, and it is the responsibility
 * of the class to save and restore the contents of its instances.
 * <p>
 * The writeTo and readFrom methods of the Message
 * interface are implemented by a class to give the class complete
 * control over the format and contents of the stream for an object
 * and its supertypes.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see java.io.Serializable
 * @see java.io.Externalizable
 * @since 1.0 2025/8/16 23:42
 */
public interface Message {

  /**
   * The object implements the writeTo method to save its contents
   * by calling the methods of {@link Writable} for its primitive values or
   * calling the write method of Output for objects, strings,
   * and arrays.
   *
   * @param writable the stream to write the object to
   * @throws SerializationException Serialization occur
   */
  void writeTo(Writable writable);

  /**
   * The object implements the readFrom method to restore its
   * contents by calling the methods of {@link Readable} for primitive
   * types and read for objects, strings and arrays.  The
   * readFrom method must read the values in the same sequence
   * and with the same types as were written by writeTo.
   *
   * @param readable the source to read data from in order to restore the object
   * @throws SerializationException Serialization occur
   */
  void readFrom(Readable readable);

  interface Factory<M> {

    /**
     * Create a new instance of the Message class, instantiating it
     * from the given Input whose data had previously been written by
     * {@link Message#writeTo Message.writeTo()}.
     *
     * @param source The Parcel to read the object's data from.
     * @return Returns a new instance of the Message class.
     * @throws SerializationException Serialization occur
     */
    M create(Readable source);

    /**
     * Create a new array of the Parcelable class.
     *
     * @param size Size of the array.
     * @return Returns an array of the Parcelable class, with every entry
     * initialized to null.
     */
    M[] newArray(int size);
  }

}
