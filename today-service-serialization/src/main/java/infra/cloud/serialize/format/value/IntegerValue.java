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

import infra.cloud.serialize.format.MessageFormat;
import infra.cloud.serialize.format.MessageIntegerOverflowException;

/**
 * Representation of MessagePack's Integer type.
 *
 * MessagePack's Integer type can represent from -2<sup>63</sup> to 2<sup>64</sup>-1.
 */
public interface IntegerValue
        extends NumberValue {
  /**
   * Returns true if the value is in the range of [-2<sup>7</sup> to 2<sup>7</sup>-1].
   */
  boolean isInByteRange();

  /**
   * Returns true if the value is in the range of [-2<sup>15</sup> to 2<sup>15</sup>-1]
   */
  boolean isInShortRange();

  /**
   * Returns true if the value is in the range of [-2<sup>31</sup> to 2<sup>31</sup>-1]
   */
  boolean isInIntRange();

  /**
   * Returns true if the value is in the range of [-2<sup>63</sup> to 2<sup>63</sup>-1]
   */
  boolean isInLongRange();

  /**
   * Returns the most succinct MessageFormat type to represent this integer value.
   *
   * @return the smallest integer type of MessageFormat that is big enough to store the value.
   */
  MessageFormat mostSuccinctMessageFormat();

  /**
   * Returns the value as a {@code byte}, otherwise throws an exception.
   *
   * @throws MessageIntegerOverflowException If the value does not fit in the range of {@code byte} type.
   */
  byte asByte();

  /**
   * Returns the value as a {@code short}, otherwise throws an exception.
   *
   * @throws MessageIntegerOverflowException If the value does not fit in the range of {@code short} type.
   */
  short asShort();

  /**
   * Returns the value as an {@code int}, otherwise throws an exception.
   *
   * @throws MessageIntegerOverflowException If the value does not fit in the range of {@code int} type.
   */
  int asInt();

  /**
   * Returns the value as a {@code long}, otherwise throws an exception.
   *
   * @throws MessageIntegerOverflowException If the value does not fit in the range of {@code long} type.
   */
  long asLong();

  /**
   * Returns the value as a {@code BigInteger}.
   */
  BigInteger asBigInteger();
}
