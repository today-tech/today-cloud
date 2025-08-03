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
