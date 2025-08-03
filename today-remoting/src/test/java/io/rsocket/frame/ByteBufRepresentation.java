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
package io.rsocket.frame;

import org.assertj.core.api.Assertions;
import org.assertj.core.presentation.StandardRepresentation;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.IllegalReferenceCountException;

public final class ByteBufRepresentation extends StandardRepresentation
        implements BeforeAllCallback {

  @Override
  public void beforeAll(ExtensionContext context) {
    Assertions.useRepresentation(this);
  }

  @Override
  protected String fallbackToStringOf(Object object) {
    if (object instanceof ByteBuf) {
      try {
        String normalBufferString = object.toString();
        ByteBuf byteBuf = (ByteBuf) object;
        if (byteBuf.readableBytes() <= 128) {
          String prettyHexDump = ByteBufUtil.prettyHexDump(byteBuf);
          return new StringBuilder()
                  .append(normalBufferString)
                  .append("\n")
                  .append(prettyHexDump)
                  .toString();
        }
        else {
          return normalBufferString;
        }
      }
      catch (IllegalReferenceCountException e) {
        // noops
      }
    }

    return super.fallbackToStringOf(object);
  }
}
