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

package infra.remoting.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

final class NumberUtilsTests {

  @DisplayName("returns int value with postitive int")
  @Test
  void requireNonNegativeInt() {
    assertThat(NumberUtils.requireNonNegative(Integer.MAX_VALUE, "test-message"))
            .isEqualTo(Integer.MAX_VALUE);
  }

  @DisplayName(
          "requireNonNegative with int argument throws IllegalArgumentException with negative value")
  @Test
  void requireNonNegativeIntNegative() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> NumberUtils.requireNonNegative(Integer.MIN_VALUE, "test-message"))
            .withMessage("test-message");
  }

  @DisplayName("requireNonNegative with int argument throws NullPointerException with null message")
  @Test
  void requireNonNegativeIntNullMessage() {
    assertThatNullPointerException()
            .isThrownBy(() -> NumberUtils.requireNonNegative(Integer.MIN_VALUE, null))
            .withMessage("message is required");
  }

  @DisplayName("requireNonNegative returns int value with zero")
  @Test
  void requireNonNegativeIntZero() {
    assertThat(NumberUtils.requireNonNegative(0, "test-message")).isEqualTo(0);
  }

  @DisplayName("requirePositive returns int value with positive int")
  @Test
  void requirePositiveInt() {
    assertThat(NumberUtils.requirePositive(Integer.MAX_VALUE, "test-message"))
            .isEqualTo(Integer.MAX_VALUE);
  }

  @DisplayName(
          "requirePositive with int argument throws IllegalArgumentException with negative value")
  @Test
  void requirePositiveIntNegative() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> NumberUtils.requirePositive(Integer.MIN_VALUE, "test-message"))
            .withMessage("test-message");
  }

  @DisplayName("requirePositive with int argument throws NullPointerException with null message")
  @Test
  void requirePositiveIntNullMessage() {
    assertThatNullPointerException()
            .isThrownBy(() -> NumberUtils.requirePositive(Integer.MIN_VALUE, null))
            .withMessage("message is required");
  }

  @DisplayName("requirePositive with int argument throws IllegalArgumentException with zero value")
  @Test
  void requirePositiveIntZero() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> NumberUtils.requirePositive(0, "test-message"))
            .withMessage("test-message");
  }

  @DisplayName("requirePositive returns long value with positive long")
  @Test
  void requirePositiveLong() {
    assertThat(NumberUtils.requirePositive(Long.MAX_VALUE, "test-message"))
            .isEqualTo(Long.MAX_VALUE);
  }

  @DisplayName(
          "requirePositive with long argument throws IllegalArgumentException with negative value")
  @Test
  void requirePositiveLongNegative() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> NumberUtils.requirePositive(Long.MIN_VALUE, "test-message"))
            .withMessage("test-message");
  }

  @DisplayName("requirePositive with long argument throws NullPointerException with null message")
  @Test
  void requirePositiveLongNullMessage() {
    assertThatNullPointerException()
            .isThrownBy(() -> NumberUtils.requirePositive(Long.MIN_VALUE, null))
            .withMessage("message is required");
  }

  @DisplayName("requirePositive with long argument throws IllegalArgumentException with zero value")
  @Test
  void requirePositiveLongZero() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> NumberUtils.requirePositive(0L, "test-message"))
            .withMessage("test-message");
  }

  @DisplayName("requireUnsignedByte returns length if 255")
  @Test
  void requireUnsignedByte() {
    assertThat(NumberUtils.requireUnsignedByte((1 << 8) - 1)).isEqualTo(255);
  }

  @DisplayName("requireUnsignedByte throws IllegalArgumentException if larger than 255")
  @Test
  void requireUnsignedByteOverFlow() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> NumberUtils.requireUnsignedByte(1 << 8))
            .withMessage("%d is larger than 8 bits", 1 << 8);
  }

  @DisplayName("requireUnsignedMedium returns length if 16_777_215")
  @Test
  void requireUnsignedMedium() {
    assertThat(NumberUtils.requireUnsignedMedium((1 << 24) - 1)).isEqualTo(16_777_215);
  }

  @DisplayName("requireUnsignedMedium throws IllegalArgumentException if larger than 16_777_215")
  @Test
  void requireUnsignedMediumOverFlow() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> NumberUtils.requireUnsignedMedium(1 << 24))
            .withMessage("%d is larger than 24 bits", 1 << 24);
  }

  @DisplayName("requireUnsignedShort returns length if 65_535")
  @Test
  void requireUnsignedShort() {
    assertThat(NumberUtils.requireUnsignedShort((1 << 16) - 1)).isEqualTo(65_535);
  }

  @DisplayName("requireUnsignedShort throws IllegalArgumentException if larger than 65_535")
  @Test
  void requireUnsignedShortOverFlow() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> NumberUtils.requireUnsignedShort(1 << 16))
            .withMessage("%d is larger than 16 bits", 1 << 16);
  }

  @Test
  void encodeUnsignedMedium() {
    ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
    NumberUtils.encodeUnsignedMedium(buffer, 129);
    buffer.markReaderIndex();

    assertThat(buffer.readUnsignedMedium()).as("reading as unsigned medium").isEqualTo(129);

    buffer.resetReaderIndex();
    assertThat(buffer.readMedium()).as("reading as signed medium").isEqualTo(129);
  }

  @Test
  void encodeUnsignedMediumLarge() {
    ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
    NumberUtils.encodeUnsignedMedium(buffer, 0xFFFFFC);
    buffer.markReaderIndex();

    assertThat(buffer.readUnsignedMedium()).as("reading as unsigned medium").isEqualTo(16777212);

    buffer.resetReaderIndex();
    assertThat(buffer.readMedium()).as("reading as signed medium").isEqualTo(-4);
  }
}
