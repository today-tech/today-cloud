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

package infra.cloud.serialize.format.buffer;

import java.io.Closeable;
import java.io.IOException;

import infra.lang.Nullable;

/**
 * Provides a sequence of MessageBuffer instances.
 * <p>
 * A MessageBufferInput implementation has control of lifecycle
 * of the memory so that it can reuse previously allocated memory,
 * use memory pools, or use memory-mapped files.
 */
public interface MessageBufferInput extends Closeable {

  /**
   * Returns a next buffer to read.
   * <p>
   * This method should return a MessageBuffer instance that has data filled in. When this method is called twice,
   * the previously returned buffer is no longer used. Thus, implementation of this method can safely discard it.
   * This is useful when it uses a memory pool.
   *
   * @return the next MessageBuffer, or return null if no more buffer is available.
   * @throws IOException when IO error occurred when reading the data
   */
  @Nullable
  MessageBuffer next() throws IOException;

  /**
   * Closes the input.
   * <p>
   * When this method is called, the buffer previously returned from {@link #next()} method is no longer used.
   * Thus, implementation of this method can safely discard it.
   * <p>
   * If the input is already closed then invoking this method has no effect.
   *
   * @throws IOException when IO error occurred when closing the data source
   */
  @Override
  void close() throws IOException;

}
