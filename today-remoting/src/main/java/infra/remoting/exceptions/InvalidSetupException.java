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
import infra.remoting.frame.ErrorFrameCodec;

/**
 * The Setup frame is invalid for the server (it could be that the client is too recent for the old
 * server).
 *
 * @see <a href="https://github.com/today-tech/today-cloud/blob/master/today-remoting/Protocol.md#error-codes">Error
 * Codes</a>
 */
public final class InvalidSetupException extends SetupException {

  private static final long serialVersionUID = -6816210006610385251L;

  /**
   * Constructs a new exception with the specified message.
   *
   * @param message the message
   */
  public InvalidSetupException(String message) {
    this(message, null);
  }

  /**
   * Constructs a new exception with the specified message and cause.
   *
   * @param message the message
   * @param cause the cause of this exception
   */
  public InvalidSetupException(String message, @Nullable Throwable cause) {
    super(ErrorFrameCodec.INVALID_SETUP, message, cause);
  }
}
