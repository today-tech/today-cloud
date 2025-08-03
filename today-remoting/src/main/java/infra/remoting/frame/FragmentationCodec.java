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

package infra.remoting.frame;

import infra.lang.Nullable;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

/** FragmentationFlyweight is used to re-assemble frames */
public class FragmentationCodec {
  public static ByteBuf encode(final ByteBufAllocator allocator, ByteBuf header, ByteBuf data) {
    return encode(allocator, header, null, data);
  }

  public static ByteBuf encode(
          final ByteBufAllocator allocator, ByteBuf header, @Nullable ByteBuf metadata, ByteBuf data) {

    final boolean hasMetadata = metadata != null;
    return FrameBodyCodec.encode(allocator, header, metadata, hasMetadata, data);
  }
}
