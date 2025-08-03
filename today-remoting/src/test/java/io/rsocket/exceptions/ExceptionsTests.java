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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.rsocket.RaceTestConstants;
import io.rsocket.frame.ErrorFrameCodec;

import static io.rsocket.frame.ErrorFrameCodec.APPLICATION_ERROR;
import static io.rsocket.frame.ErrorFrameCodec.CANCELED;
import static io.rsocket.frame.ErrorFrameCodec.CONNECTION_CLOSE;
import static io.rsocket.frame.ErrorFrameCodec.CONNECTION_ERROR;
import static io.rsocket.frame.ErrorFrameCodec.INVALID;
import static io.rsocket.frame.ErrorFrameCodec.INVALID_SETUP;
import static io.rsocket.frame.ErrorFrameCodec.REJECTED;
import static io.rsocket.frame.ErrorFrameCodec.REJECTED_RESUME;
import static io.rsocket.frame.ErrorFrameCodec.REJECTED_SETUP;
import static io.rsocket.frame.ErrorFrameCodec.UNSUPPORTED_SETUP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

final class ExceptionsTests {
  @DisplayName("from returns ApplicationErrorException")
  @Test
  void fromApplicationException() {
    ByteBuf byteBuf = createErrorFrame(1, APPLICATION_ERROR, "test-message");

    try {
      assertThat(Exceptions.from(1, byteBuf))
              .isInstanceOf(ApplicationErrorException.class)
              .hasMessage("test-message");

      assertThat(Exceptions.from(0, byteBuf))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessage(
                      "Invalid Error frame in Stream ID 0: 0x%08X '%s'", APPLICATION_ERROR, "test-message");
    }
    finally {
      byteBuf.release();
    }
  }

  @DisplayName("from returns CanceledException")
  @Test
  void fromCanceledException() {
    ByteBuf byteBuf = createErrorFrame(1, CANCELED, "test-message");

    try {

      assertThat(Exceptions.from(1, byteBuf))
              .isInstanceOf(CanceledException.class)
              .hasMessage("test-message");

      assertThat(Exceptions.from(0, byteBuf))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessage("Invalid Error frame in Stream ID 0: 0x%08X '%s'", CANCELED, "test-message");
    }
    finally {
      byteBuf.release();
    }
  }

  @DisplayName("from returns ConnectionCloseException")
  @Test
  void fromConnectionCloseException() {
    ByteBuf byteBuf = createErrorFrame(0, CONNECTION_CLOSE, "test-message");
    try {

      assertThat(Exceptions.from(0, byteBuf))
              .isInstanceOf(ConnectionCloseException.class)
              .hasMessage("test-message");

      assertThat(Exceptions.from(1, byteBuf))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessage(
                      "Invalid Error frame in Stream ID 1: 0x%08X '%s'", CONNECTION_CLOSE, "test-message");
    }
    finally {
      byteBuf.release();
    }
  }

  @DisplayName("from returns ConnectionErrorException")
  @Test
  void fromConnectionErrorException() {
    ByteBuf byteBuf = createErrorFrame(0, CONNECTION_ERROR, "test-message");

    try {

      assertThat(Exceptions.from(0, byteBuf))
              .isInstanceOf(ConnectionErrorException.class)
              .hasMessage("test-message");

      assertThat(Exceptions.from(1, byteBuf))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessage(
                      "Invalid Error frame in Stream ID 1: 0x%08X '%s'", CONNECTION_ERROR, "test-message");
    }
    finally {
      byteBuf.release();
    }
  }

  @DisplayName("from returns IllegalArgumentException if error frame has illegal error code")
  @Test
  void fromIllegalErrorFrame() {
    ByteBuf byteBuf = createErrorFrame(0, 0x00000000, "test-message");
    try {

      assertThat(Exceptions.from(0, byteBuf))
              .hasMessage("Invalid Error frame in Stream ID 0: 0x%08X '%s'", 0, "test-message")
              .isInstanceOf(IllegalArgumentException.class);

      assertThat(Exceptions.from(1, byteBuf))
              .hasMessage("Invalid Error frame in Stream ID 1: 0x%08X '%s'", 0x00000000, "test-message")
              .isInstanceOf(IllegalArgumentException.class);
    }
    finally {
      byteBuf.release();
    }
  }

  @DisplayName("from returns InvalidException")
  @Test
  void fromInvalidException() {
    ByteBuf byteBuf = createErrorFrame(1, INVALID, "test-message");
    try {
      assertThat(Exceptions.from(1, byteBuf))
              .isInstanceOf(InvalidException.class)
              .hasMessage("test-message");

      assertThat(Exceptions.from(0, byteBuf))
              .hasMessage("Invalid Error frame in Stream ID 0: 0x%08X '%s'", INVALID, "test-message")
              .isInstanceOf(IllegalArgumentException.class);
    }
    finally {
      byteBuf.release();
    }
  }

  @DisplayName("from returns InvalidSetupException")
  @Test
  void fromInvalidSetupException() {
    ByteBuf byteBuf = createErrorFrame(0, INVALID_SETUP, "test-message");
    try {
      assertThat(Exceptions.from(0, byteBuf))
              .isInstanceOf(InvalidSetupException.class)
              .hasMessage("test-message");

      assertThat(Exceptions.from(1, byteBuf))
              .hasMessage(
                      "Invalid Error frame in Stream ID 1: 0x%08X '%s'", INVALID_SETUP, "test-message")
              .isInstanceOf(IllegalArgumentException.class);
    }
    finally {
      byteBuf.release();
    }
  }

  @DisplayName("from returns RejectedException")
  @Test
  void fromRejectedException() {
    ByteBuf byteBuf = createErrorFrame(1, REJECTED, "test-message");
    try {

      assertThat(Exceptions.from(1, byteBuf))
              .isInstanceOf(RejectedException.class)
              .withFailMessage("test-message");

      assertThat(Exceptions.from(0, byteBuf))
              .hasMessage("Invalid Error frame in Stream ID 0: 0x%08X '%s'", REJECTED, "test-message")
              .isInstanceOf(IllegalArgumentException.class);
    }
    finally {
      byteBuf.release();
    }
  }

  @DisplayName("from returns RejectedResumeException")
  @Test
  void fromRejectedResumeException() {
    ByteBuf byteBuf = createErrorFrame(0, REJECTED_RESUME, "test-message");
    try {

      assertThat(Exceptions.from(0, byteBuf))
              .isInstanceOf(RejectedResumeException.class)
              .hasMessage("test-message");

      assertThat(Exceptions.from(1, byteBuf))
              .hasMessage(
                      "Invalid Error frame in Stream ID 1: 0x%08X '%s'", REJECTED_RESUME, "test-message")
              .isInstanceOf(IllegalArgumentException.class);
    }
    finally {
      byteBuf.release();
    }
  }

  @DisplayName("from returns RejectedSetupException")
  @Test
  void fromRejectedSetupException() {
    ByteBuf byteBuf = createErrorFrame(0, REJECTED_SETUP, "test-message");
    try {

      assertThat(Exceptions.from(0, byteBuf))
              .isInstanceOf(RejectedSetupException.class)
              .withFailMessage("test-message");

      assertThat(Exceptions.from(1, byteBuf))
              .hasMessage(
                      "Invalid Error frame in Stream ID 1: 0x%08X '%s'", REJECTED_SETUP, "test-message")
              .isInstanceOf(IllegalArgumentException.class);
    }
    finally {
      byteBuf.release();
    }
  }

  @DisplayName("from returns UnsupportedSetupException")
  @Test
  void fromUnsupportedSetupException() {
    ByteBuf byteBuf = createErrorFrame(0, UNSUPPORTED_SETUP, "test-message");
    try {
      assertThat(Exceptions.from(0, byteBuf))
              .isInstanceOf(UnsupportedSetupException.class)
              .hasMessage("test-message");

      assertThat(Exceptions.from(1, byteBuf))
              .hasMessage(
                      "Invalid Error frame in Stream ID 1: 0x%08X '%s'", UNSUPPORTED_SETUP, "test-message")
              .isInstanceOf(IllegalArgumentException.class);
    }
    finally {
      byteBuf.release();
    }
  }

  @DisplayName("from returns CustomRSocketException")
  @Test
  void fromCustomRSocketException() {
    for (int i = 0; i < RaceTestConstants.REPEATS; i++) {
      int randomCode =
              ThreadLocalRandom.current().nextBoolean()
                      ? ThreadLocalRandom.current()
                      .nextInt(Integer.MIN_VALUE, ErrorFrameCodec.MAX_USER_ALLOWED_ERROR_CODE)
                      : ThreadLocalRandom.current()
                              .nextInt(ErrorFrameCodec.MIN_USER_ALLOWED_ERROR_CODE, Integer.MAX_VALUE);
      ByteBuf byteBuf = createErrorFrame(0, randomCode, "test-message");
      try {
        assertThat(Exceptions.from(1, byteBuf))
                .isInstanceOf(CustomRSocketException.class)
                .hasMessage("test-message");

        assertThat(Exceptions.from(0, byteBuf))
                .hasMessage(
                        "Invalid Error frame in Stream ID 0: 0x%08X '%s'", randomCode, "test-message")
                .isInstanceOf(IllegalArgumentException.class);
      }
      finally {
        byteBuf.release();
      }
    }
  }

  @DisplayName("from throws NullPointerException with null frame")
  @Test
  void fromWithNullFrame() {
    assertThatNullPointerException()
            .isThrownBy(() -> Exceptions.from(0, null))
            .withMessage("frame is required");
  }

  private ByteBuf createErrorFrame(int streamId, int errorCode, String message) {
    return ErrorFrameCodec.encode(
            UnpooledByteBufAllocator.DEFAULT, streamId, new TestRSocketException(errorCode, message));
  }
}
