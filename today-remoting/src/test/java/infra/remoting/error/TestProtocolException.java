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

package infra.remoting.error;

import infra.remoting.ProtocolErrorException;
import infra.remoting.frame.ErrorFrameCodec;

public class TestProtocolException extends ProtocolErrorException {
  private static final long serialVersionUID = 7873267740343446585L;

  private final int errorCode;

  /**
   * Constructs a new exception with the specified message.
   *
   * @param errorCode customizable error code
   * @param message the message
   * @throws NullPointerException if {@code message} is {@code null}
   * @throws IllegalArgumentException if {@code errorCode} is out of allowed range
   */
  public TestProtocolException(int errorCode, String message) {
    super(ErrorFrameCodec.APPLICATION_ERROR, message);
    this.errorCode = errorCode;
  }

  /**
   * Constructs a new exception with the specified message and cause.
   *
   * @param errorCode customizable error code
   * @param message the message
   * @param cause the cause of this exception
   * @throws NullPointerException if {@code message} or {@code cause} is {@code null}
   * @throws IllegalArgumentException if {@code errorCode} is out of allowed range
   */
  public TestProtocolException(int errorCode, String message, Throwable cause) {
    super(ErrorFrameCodec.APPLICATION_ERROR, message, cause);
    this.errorCode = errorCode;
  }

  @Override
  public int errorCode() {
    return errorCode;
  }
}
