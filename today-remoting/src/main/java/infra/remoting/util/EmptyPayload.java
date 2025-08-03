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

package infra.remoting.util;

import infra.remoting.Payload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class EmptyPayload implements Payload {
  public static final EmptyPayload INSTANCE = new EmptyPayload();

  private EmptyPayload() { }

  @Override
  public boolean hasMetadata() {
    return false;
  }

  @Override
  public ByteBuf sliceMetadata() {
    return Unpooled.EMPTY_BUFFER;
  }

  @Override
  public ByteBuf sliceData() {
    return Unpooled.EMPTY_BUFFER;
  }

  @Override
  public ByteBuf data() {
    return sliceData();
  }

  @Override
  public ByteBuf metadata() {
    return sliceMetadata();
  }

  @Override
  public int refCnt() {
    return 1;
  }

  @Override
  public EmptyPayload retain() {
    return this;
  }

  @Override
  public EmptyPayload retain(int increment) {
    return this;
  }

  @Override
  public EmptyPayload touch() {
    return this;
  }

  @Override
  public EmptyPayload touch(Object hint) {
    return this;
  }

  @Override
  public boolean release() {
    return false;
  }

  @Override
  public boolean release(int decrement) {
    return false;
  }
}
