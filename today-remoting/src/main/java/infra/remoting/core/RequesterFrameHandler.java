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
package infra.remoting.core;

import java.util.concurrent.CancellationException;

import infra.lang.Nullable;
import infra.remoting.Payload;
import io.netty.buffer.CompositeByteBuf;

interface RequesterFrameHandler extends FrameHandler {

  void handlePayload(Payload payload);

  @Override
  default void handleCancel() {
    handleError(
            new CancellationException(
                    "Cancellation was received but should not be possible for current request type"));
  }

  @Override
  default void handleRequestN(long n) {
    // no ops
  }

  @Nullable
  CompositeByteBuf getFrames();

  void setFrames(@Nullable CompositeByteBuf reassembledFrames);
}
