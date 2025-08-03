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

package io.rsocket;

import infra.lang.Nullable;

/**
 * Exception that represents an RSocket protocol error.
 *
 * @see <a href="https://github.com/rsocket/rsocket/blob/master/Protocol.md#error-frame-0x0b">ERROR
 * Frame (0x0B)</a>
 */
public class RSocketErrorException extends RuntimeException {

  private static final long serialVersionUID = -1628781753426267554L;

  private static final int MIN_ERROR_CODE = 0x00000001;

  private static final int MAX_ERROR_CODE = 0xFFFFFFFE;

  private final int errorCode;

  /**
   * Constructor with a protocol error code and a message.
   *
   * @param errorCode the RSocket protocol error code
   * @param message error explanation
   */
  public RSocketErrorException(int errorCode, String message) {
    this(errorCode, message, null);
  }

  /**
   * Alternative to {@link #RSocketErrorException(int, String)} with a root cause.
   *
   * @param errorCode the RSocket protocol error code
   * @param message error explanation
   * @param cause a root cause for the error
   */
  public RSocketErrorException(int errorCode, String message, @Nullable Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
    if (errorCode > MAX_ERROR_CODE && errorCode < MIN_ERROR_CODE) {
      throw new IllegalArgumentException(
              "Allowed errorCode value should be in range [0x00000001-0xFFFFFFFE]", this);
    }
  }

  /**
   * Return the RSocket <a
   * href="https://github.com/rsocket/rsocket/blob/master/Protocol.md#error-codes">error code</a>
   * represented by this exception
   *
   * @return the RSocket protocol error code
   */
  public int errorCode() {
    return errorCode;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
            + " (0x"
            + Integer.toHexString(errorCode)
            + "): "
            + getMessage();
  }
}
