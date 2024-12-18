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

package infra.cloud.protocol;

import infra.lang.Nullable;

/**
 * Protocol parsing exception
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/8 22:05
 */
public class ProtocolParsingException extends IllegalArgumentException {

  /**
   * Constructs an {@code ProtocolParsingException} with the
   * specified detail message.
   */
  public ProtocolParsingException(@Nullable String s) {
    super(s);
  }

  /**
   * Constructs a new exception with the specified detail message and
   * cause.
   */
  public ProtocolParsingException(@Nullable String message, @Nullable Throwable cause) {
    super(message, cause);
  }

}
