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

import java.util.Objects;

import infra.remoting.ProtocolErrorException;
import infra.remoting.frame.ErrorFrameCodec;
import io.netty.buffer.ByteBuf;

import static infra.remoting.frame.ErrorFrameCodec.APPLICATION_ERROR;
import static infra.remoting.frame.ErrorFrameCodec.CANCELED;
import static infra.remoting.frame.ErrorFrameCodec.CONNECTION_CLOSE;
import static infra.remoting.frame.ErrorFrameCodec.CONNECTION_ERROR;
import static infra.remoting.frame.ErrorFrameCodec.INVALID;
import static infra.remoting.frame.ErrorFrameCodec.INVALID_SETUP;
import static infra.remoting.frame.ErrorFrameCodec.MAX_USER_ALLOWED_ERROR_CODE;
import static infra.remoting.frame.ErrorFrameCodec.MIN_USER_ALLOWED_ERROR_CODE;
import static infra.remoting.frame.ErrorFrameCodec.REJECTED;
import static infra.remoting.frame.ErrorFrameCodec.REJECTED_RESUME;
import static infra.remoting.frame.ErrorFrameCodec.REJECTED_SETUP;
import static infra.remoting.frame.ErrorFrameCodec.UNSUPPORTED_SETUP;

/** Utility class that generates an exception from a frame. */
public final class Exceptions {

  private Exceptions() {
  }

  /**
   * Create a {@link ProtocolErrorException} from a Frame that matches the error code it contains.
   *
   * @param frame the frame to retrieve the error code and message from
   * @return a {@link ProtocolErrorException} that matches the error code in the Frame
   * @throws NullPointerException if {@code frame} is {@code null}
   */
  public static RuntimeException from(int streamId, ByteBuf frame) {
    Objects.requireNonNull(frame, "frame is required");

    int errorCode = ErrorFrameCodec.errorCode(frame);
    String message = ErrorFrameCodec.dataUtf8(frame);

    if (streamId == 0) {
      return switch (errorCode) {
        case INVALID_SETUP -> new InvalidSetupException(message);
        case UNSUPPORTED_SETUP -> new UnsupportedSetupException(message);
        case REJECTED_SETUP -> new RejectedSetupException(message);
        case REJECTED_RESUME -> new RejectedResumeException(message);
        case CONNECTION_ERROR -> new ConnectionErrorException(message);
        case CONNECTION_CLOSE -> new ConnectionCloseException(message);
        default -> new IllegalArgumentException(String.format("Invalid Error frame in Stream ID 0: 0x%08X '%s'", errorCode, message));
      };
    }
    else {
      return switch (errorCode) {
        case APPLICATION_ERROR -> new ApplicationErrorException(message);
        case REJECTED -> new RejectedException(message);
        case CANCELED -> new CanceledException(message);
        case INVALID -> new InvalidException(message);
        default -> {
          if (errorCode >= MIN_USER_ALLOWED_ERROR_CODE
                  || errorCode <= MAX_USER_ALLOWED_ERROR_CODE) {
            yield new CustomProtocolException(errorCode, message);
          }
          yield new IllegalArgumentException(String.format("Invalid Error frame in Stream ID %d: 0x%08X '%s'", streamId, errorCode, message));
        }
      };
    }
  }
}
