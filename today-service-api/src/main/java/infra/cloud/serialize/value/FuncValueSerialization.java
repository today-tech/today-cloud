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

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/3/8 21:37
 */
final class FuncValueSerialization<T> implements ValueSerialization<T> {

  private final Function<Input, T> reader;

  private final BiConsumer<Output, T> writer;

  FuncValueSerialization(Function<Input, T> reader, BiConsumer<Output, T> writer) {
    this.reader = reader;
    this.writer = writer;
  }

  @Override
  public void serialize(MethodParameter parameter, T value, Output payload) {
    writer.accept(payload, value);
  }

  @Override
  public T deserialize(MethodParameter parameter, Input input) throws SerializationException {
    return reader.apply(input);
  }

}
