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

package infra.remoting.exceptions;

import infra.lang.Nullable;
import infra.remoting.RSocketErrorException;
import infra.remoting.frame.ErrorFrameCodec;

public class CustomRSocketException extends RSocketErrorException {
  private static final long serialVersionUID = 7873267740343446585L;

  /**
   * Constructs a new exception with the specified message.
   *
   * @param errorCode customizable error code. Should be in range [0x00000301-0xFFFFFFFE]
   * @param message the message
   * @throws IllegalArgumentException if {@code errorCode} is out of allowed range
   */
  public CustomRSocketException(int errorCode, String message) {
    this(errorCode, message, null);
  }

  /**
   * Constructs a new exception with the specified message and cause.
   *
   * @param errorCode customizable error code. Should be in range [0x00000301-0xFFFFFFFE]
   * @param message the message
   * @param cause the cause of this exception
   * @throws IllegalArgumentException if {@code errorCode} is out of allowed range
   */
  public CustomRSocketException(int errorCode, String message, @Nullable Throwable cause) {
    super(errorCode, message, cause);
    if (errorCode > ErrorFrameCodec.MAX_USER_ALLOWED_ERROR_CODE
            && errorCode < ErrorFrameCodec.MIN_USER_ALLOWED_ERROR_CODE) {
      throw new IllegalArgumentException(
              "Allowed errorCode value should be in range [0x00000301-0xFFFFFFFE]", this);
    }
  }
}
