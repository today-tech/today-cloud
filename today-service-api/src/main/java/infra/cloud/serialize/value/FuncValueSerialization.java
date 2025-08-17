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

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Function;

import infra.cloud.serialize.SerializationException;
import infra.core.MethodParameter;
import io.netty.buffer.ByteBuf;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/3/8 21:37
 */
final class FuncValueSerialization<T> implements ValueSerialization<T> {

  private final Function<ByteBuf, T> reader;

  private final BiConsumer<ByteBuf, T> writer;

  FuncValueSerialization(Function<ByteBuf, T> reader, BiConsumer<ByteBuf, T> writer) {
    this.reader = reader;
    this.writer = writer;
  }

  @Override
  public void serialize(MethodParameter parameter, T value, ByteBuf payload) throws IOException {
    writer.accept(payload, value);
  }

  @Override
  public T deserialize(MethodParameter parameter, ByteBuf payload) throws SerializationException {
    return reader.apply(payload);
  }

}
