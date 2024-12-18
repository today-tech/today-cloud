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

package infra.cloud.core.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

/**
 * @author TODAY 2021/7/8 23:04
 */
public class JdkDeserializer implements Deserializer {

  @Override
  public Object deserialize(final InputStream inputStream) throws IOException {
    try (final ObjectInputStream objectInput = new ObjectInputStream(inputStream)) {
      return objectInput.readObject();
    }
    catch (ClassNotFoundException e) {
      throw new IOException("target type not in classpath", e);
    }
  }

}
