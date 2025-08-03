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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import infra.remoting.RSocketErrorException;

import static org.assertj.core.api.Assertions.assertThat;

interface ChannelExceptionTests<T extends RSocketErrorException> {

  @DisplayName("constructor does not throw NullPointerException with null message")
  @Test
  default void constructorWithNullMessage() {
    assertThat(getException(null)).hasMessage(null);
  }

  @DisplayName("constructor does not throw NullPointerException with null message and cause")
  @Test
  default void constructorWithNullMessageAndCause() {
    assertThat(getException(null)).hasMessage(null);
  }

  @DisplayName("errorCode returns specified value")
  @Test
  default void errorCodeReturnsSpecifiedValue() {
    assertThat(getException("test-message").errorCode()).isEqualTo(getSpecifiedErrorCode());
  }

  T getException(String message, Throwable cause);

  T getException(String message);

  int getSpecifiedErrorCode();
}
