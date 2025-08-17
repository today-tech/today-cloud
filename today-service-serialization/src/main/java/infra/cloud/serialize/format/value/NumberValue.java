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

package infra.cloud.serialize.format.value;

import java.math.BigInteger;

/**
 * Base interface of {@link IntegerValue} and {@link FloatValue} interfaces. To extract primitive type values, call toXXX methods, which may lose some information by rounding or truncation.
 *
 * @see IntegerValue
 * @see FloatValue
 */
public interface NumberValue extends Value {

  /**
   * Represent this value as a byte value, which may involve rounding or truncation of the original value.
   * the value.
   */
  byte toByte();

  /**
   * Represent this value as a short value, which may involve rounding or truncation of the original value.
   */
  short toShort();

  /**
   * Represent this value as an int value, which may involve rounding or truncation of the original value.
   * value.
   */
  int toInt();

  /**
   * Represent this value as a long value, which may involve rounding or truncation of the original value.
   */
  long toLong();

  /**
   * Represent this value as a BigInteger, which may involve rounding or truncation of the original value.
   */
  BigInteger toBigInteger();

  /**
   * Represent this value as a 32-bit float value, which may involve rounding or truncation of the original value.
   */
  float toFloat();

  /**
   * Represent this value as a 64-bit double value, which may involve rounding or truncation of the original value.
   */
  double toDouble();
}
