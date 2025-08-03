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
 * The server rejected the resume, it can specify the reason in the payload.
 *
 * @see <a href="https://github.com/rsocket/rsocket/blob/master/Protocol.md#error-codes">Error
 * Codes</a>
 */
public final class RejectedResumeException extends RSocketErrorException {

  private static final long serialVersionUID = -873684362478544811L;

  /**
   * Constructs a new exception with the specified message.
   *
   * @param message the message
   */
  public RejectedResumeException(String message) {
    this(message, null);
  }

  /**
   * Constructs a new exception with the specified message and cause.
   *
   * @param message the message
   * @param cause the cause of this exception
   */
  public RejectedResumeException(String message, @Nullable Throwable cause) {
    super(ErrorFrameCodec.REJECTED_RESUME, message, cause);
  }
}
