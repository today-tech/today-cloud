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

package infra.remoting;

import infra.lang.Nullable;

/**
 * Exception that represents a protocol error.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see <a href="https://github.com/today-tech/today-cloud/blob/master/today-remoting/Protocol.md#error-frame-0x0b">ERROR
 * Frame (0x0B)</a>
 */
public class ProtocolErrorException extends RemotingException {

  private static final int MIN_ERROR_CODE = 0x00000001;

  private static final int MAX_ERROR_CODE = 0xFFFFFFFE;

  private final int errorCode;

  /**
   * Constructor with a protocol error code and a message.
   *
   * @param errorCode the RSocket protocol error code
   * @param message error explanation
   */
  public ProtocolErrorException(int errorCode, String message) {
    this(errorCode, message, null);
  }

  /**
   * Alternative to {@link #ProtocolErrorException(int, String)} with a root cause.
   *
   * @param errorCode the RSocket protocol error code
   * @param message error explanation
   * @param cause a root cause for the error
   */
  public ProtocolErrorException(int errorCode, String message, @Nullable Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
    if (errorCode > MAX_ERROR_CODE && errorCode < MIN_ERROR_CODE) {
      throw new IllegalArgumentException(
              "Allowed errorCode value should be in range [0x00000001-0xFFFFFFFE]", this);
    }
  }

  /**
   * Return the Protocol <a
   * href="https://github.com/today-tech/today-cloud/blob/master/today-remoting/Protocol.md#error-codes">error code</a>
   * represented by this exception
   *
   * @return the protocol error code
   */
  public int errorCode() {
    return errorCode;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
            + " (0x" + Integer.toHexString(errorCode) + "): "
            + getMessage();
  }

}
