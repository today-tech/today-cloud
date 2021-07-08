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
import java.io.OutputStream;

/**
 * A strategy interface for streaming an object to an OutputStream.
 *
 * @author Gary Russell
 * @author Mark Fisher
 * @author TODAY 2021/7/8 22:52
 */
@FunctionalInterface
public interface Serializer {

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
  void serialize(Object object, OutputStream output) throws IOException;

}
