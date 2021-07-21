/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.rpc.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * For streaming an object to an OutputStream and
 * for converting from data in an InputStream to an Object
 *
 * @author TODAY 2021/7/8 23:04
 */
public abstract class Serialization<T> implements Serializer, Deserializer {

  /**
   * Write an object of type T to the given OutputStream.
   * <p>Note: Implementations should not close the given OutputStream
   * (or any decorators of that OutputStream) but rather leave this up
   * to the caller.
   *
   * @param object
   *         the object to serialize
   * @param output
   *         the output stream
   *
   * @throws IOException
   *         in case of errors writing to the stream
   */
  @Override
  public abstract void serialize(Object object, OutputStream output) throws IOException;

  /**
   * Read (assemble) an object of type T from the given InputStream.
   * <p>Note: Implementations should not close the given InputStream
   * (or any decorators of that InputStream) but rather leave this up
   * to the caller.
   *
   * @param inputStream
   *         the input stream
   *
   * @return the deserialized object
   *
   * @throws IOException
   *         in case of errors reading from the stream
   * @throws ClassNotFoundException
   *         if target type not in classpath
   */
  @Override
  @SuppressWarnings("unchecked")
  public T deserialize(InputStream inputStream) throws IOException, ClassNotFoundException {
    return (T) deserializeInternal(inputStream);
  }

  protected Object deserializeInternal(InputStream inputStream) throws IOException, ClassNotFoundException {
    throw new UnsupportedOperationException();
  }

}
