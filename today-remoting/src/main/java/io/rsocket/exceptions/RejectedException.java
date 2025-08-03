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

package io.rsocket.exceptions;

import infra.lang.Nullable;
import io.rsocket.RSocketErrorException;
import io.rsocket.frame.ErrorFrameCodec;

/**
 * Despite being a valid request, the Responder decided to reject it. The Responder guarantees that
 * it didn't process the request. The reason for the rejection is explained in the Error Data
 * section.
 *
 * @see <a href="https://github.com/rsocket/rsocket/blob/master/Protocol.md#error-codes">Error
 * Codes</a>
 */
public class RejectedException extends RSocketErrorException implements Retryable {

  private static final long serialVersionUID = 3926231092835143715L;

  /**
   * Constructs a new exception with the specified message.
   *
   * @param message the message
   */
  public RejectedException(String message) {
    this(message, null);
  }

  /**
   * Constructs a new exception with the specified message and cause.
   *
   * @param message the message
   * @param cause the cause of this exception
   */
  public RejectedException(String message, @Nullable Throwable cause) {
    super(ErrorFrameCodec.REJECTED, message, cause);
  }
}
