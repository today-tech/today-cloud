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

import infra.core.MethodParameter;
import infra.lang.Nullable;
import io.netty.buffer.ByteBuf;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/12/20 16:29
 */
public interface RpcArgumentSerialization<T> {

  /**
   * Whether the given parameter is supported by this resolver.
   * <p>
   * static match
   * </p>
   */
  boolean supportsArgument(MethodParameter parameter);

  void serialize(MethodParameter parameter, @Nullable T value, ByteBuf payload, Output output) throws SerializationException;

  @Nullable
  T deserialize(MethodParameter parameter, ByteBuf payload, Input input) throws SerializationException;

}
