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
import infra.remoting.ProtocolErrorException;
import infra.remoting.frame.ErrorFrameCodec;

/**
 * The connection is being terminated. Sender or Receiver of this frame MUST wait for outstanding
 * streams to terminate before closing the connection. New requests MAY not be accepted.
 *
 * @see <a href="https://github.com/today-tech/today-cloud/blob/master/today-remoting/Protocol.md#error-codes">Error
 * Codes</a>
 */
public final class ConnectionCloseException extends ProtocolErrorException {

  private static final long serialVersionUID = -2214953527482377471L;

  /**
   * Constructs a new exception with the specified message.
   *
   * @param message the message
   */
  public ConnectionCloseException(String message) {
    this(message, null);
  }

  /**
   * Constructs a new exception with the specified message and cause.
   *
   * @param message the message
   * @param cause the cause of this exception
   */
  public ConnectionCloseException(String message, @Nullable Throwable cause) {
    super(ErrorFrameCodec.CONNECTION_CLOSE, message, cause);
  }
}
