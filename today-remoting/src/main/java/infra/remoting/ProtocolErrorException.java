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

package infra.remoting;

import org.jspecify.annotations.Nullable;

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
   * @param errorCode the protocol error code
   * @param message error explanation
   */
  public ProtocolErrorException(int errorCode, String message) {
    this(errorCode, message, null);
  }

  /**
   * Alternative to {@link #ProtocolErrorException(int, String)} with a root cause.
   *
   * @param errorCode the protocol error code
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
