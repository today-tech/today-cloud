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

package infra.cloud.serialize.format;

/**
 * Thrown to indicate too large message size (e.g, larger than 2^31-1).
 */
public class MessageSizeException extends MessagePackException {

  private final long size;

  public MessageSizeException(long size) {
    super(null, null);
    this.size = size;
  }

  public MessageSizeException(String message, long size) {
    super(message, null);
    this.size = size;
  }

  public long getSize() {
    return size;
  }
}
