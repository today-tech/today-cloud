/*
 * Copyright 2021 - 2026 the TODAY authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.cloud.serialize.value;

import java.util.function.BiConsumer;
import java.util.function.Function;

import infra.cloud.serialize.Readable;
import infra.cloud.serialize.Writable;
import infra.cloud.serialize.SerializationException;
import infra.core.MethodParameter;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/3/8 21:37
 */
final class FuncValueSerialization<T> implements ValueSerialization<T> {

  private final Function<Readable, T> reader;

  private final BiConsumer<Writable, T> writer;

  FuncValueSerialization(Function<Readable, T> reader, BiConsumer<Writable, T> writer) {
    this.reader = reader;
    this.writer = writer;
  }

  @Override
  public void serialize(MethodParameter parameter, T value, Writable writable) {
    writer.accept(writable, value);
  }

  @Override
  public T deserialize(MethodParameter parameter, Readable readable) throws SerializationException {
    return reader.apply(readable);
  }

}
