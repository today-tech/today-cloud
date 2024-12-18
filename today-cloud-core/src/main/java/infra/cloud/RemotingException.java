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

package infra.cloud;

import java.io.Serial;

import infra.lang.Nullable;

/**
 * Exception for default remoting problems
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/10/19 11:53
 */
public class RemotingException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new remoting exception with {@code null} as its
   * detail message.  The cause is not initialized, and may subsequently be
   * initialized by a call to {@link #initCause}.
   */
  public RemotingException() {
    super();
  }

  /**
   * Constructs a new remoting exception with the specified detail message.
   * The cause is not initialized, and may subsequently be initialized by a
   * call to {@link #initCause}.
   *
   * @param message the detail message. The detail message is saved for
   * later retrieval by the {@link #getMessage()} method.
   */
  public RemotingException(@Nullable String message) {
    super(message);
  }

  /**
   * Constructs a new remoting exception with the specified detail message and
   * cause.  <p>Note that the detail message associated with
   * {@code cause} is <i>not</i> automatically incorporated in
   * this remoting exception's detail message.
   *
   * @param message the detail message (which is saved for later retrieval
   * by the {@link #getMessage()} method).
   * @param cause the cause (which is saved for later retrieval by the
   * {@link #getCause()} method).  (A {@code null} value is
   * permitted, and indicates that the cause is nonexistent or
   * unknown.)
   */
  public RemotingException(@Nullable String message, @Nullable Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new remoting exception with the specified cause and a
   * detail message of {@code (cause==null ? null : cause.toString())}
   * (which typically contains the class and detail message of
   * {@code cause}).  This constructor is useful for remoting exceptions
   * that are little more than wrappers for other throwables.
   *
   * @param cause the cause (which is saved for later retrieval by the
   * {@link #getCause()} method).  (A {@code null} value is
   * permitted, and indicates that the cause is nonexistent or
   * unknown.)
   */
  public RemotingException(@Nullable Throwable cause) {
    super(cause);
  }

}
