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

import org.jspecify.annotations.Nullable;

import infra.remoting.ProtocolErrorException;
import infra.remoting.frame.ErrorFrameCodec;

/**
 * The server rejected the resume, it can specify the reason in the payload.
 *
 * @see <a href="https://github.com/today-tech/today-cloud/blob/master/today-remoting/Protocol.md#error-codes">Error
 * Codes</a>
 */
public final class RejectedResumeException extends ProtocolErrorException {

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
