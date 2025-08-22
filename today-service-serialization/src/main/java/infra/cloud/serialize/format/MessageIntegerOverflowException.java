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

package infra.cloud.serialize.format;

import java.math.BigInteger;

/**
 * This error is thrown when the user tries to read an integer value
 * using a smaller types. For example, calling MessageUnpacker.unpackInt() for an integer value
 * that is larger than Integer.MAX_VALUE will cause this exception.
 */
public class MessageIntegerOverflowException extends MessageTypeException {

  private final BigInteger bigInteger;

  public MessageIntegerOverflowException(BigInteger bigInteger) {
    super(null, null);
    this.bigInteger = bigInteger;
  }

  public MessageIntegerOverflowException(long value) {
    this(BigInteger.valueOf(value));
  }

  public BigInteger getBigInteger() {
    return bigInteger;
  }

  @Override
  public String getMessage() {
    return bigInteger.toString();
  }
}
