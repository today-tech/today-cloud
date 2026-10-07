/*
 * Copyright 2021 - 2026 the TODAY authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.cloud;

import org.jspecify.annotations.Nullable;

import java.io.Serial;

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
