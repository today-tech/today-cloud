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

import java.io.IOException;
import java.io.Serial;
import java.net.SocketException;

/**
 * An exception marking prematurely or unexpectedly closed inbound.
 */
public class AbortedException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 6091789064032301718L;

  static final String CONNECTION_CLOSED_BEFORE_SEND = "Connection has been closed BEFORE send operation";

  public AbortedException(String message) {
    super(message);
  }

  public AbortedException(Throwable throwable) {
    super(throwable);
  }

  /**
   * Return true if connection has been simply aborted on a tcp level by verifying if
   * the given inbound error.
   *
   * @param err an inbound exception
   * @return true if connection has been simply aborted on a tcp level
   */
  public static boolean isConnectionReset(Throwable err) {
    return (err instanceof AbortedException && CONNECTION_CLOSED_BEFORE_SEND.equals(err.getMessage())) ||
            (err instanceof IOException && (err.getMessage() == null || err.getMessage()
                    .contains("Broken pipe") ||
                    err.getMessage().contains("Connection reset by peer"))) ||
            (err instanceof SocketException && err.getMessage() != null && err.getMessage().contains("Connection reset by peer"));
  }

  public static AbortedException beforeSend() {
    return new AbortedException(CONNECTION_CLOSED_BEFORE_SEND);
  }

}
