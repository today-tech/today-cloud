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

package infra.cloud.serialize.value;

import java.util.function.BiConsumer;
import java.util.function.Function;

import infra.cloud.serialize.Input;
import infra.cloud.serialize.Output;
import infra.cloud.serialize.SerializationException;
import infra.core.MethodParameter;
import infra.lang.Assert;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/3/8 21:31
 */
public interface ValueSerialization<T> {

  void serialize(MethodParameter parameter, T value, Output payload)
          throws SerializationException;

  T deserialize(MethodParameter parameter, Input payload)
          throws SerializationException;

  static <T> ValueSerialization<T> map(Function<Input, T> reader, BiConsumer<Output, T> writer) {
    Assert.notNull(reader, "reader Function is required");
    Assert.notNull(writer, "writer BiConsumer is required");
    return new FuncValueSerialization<>(reader, writer);
  }

}

