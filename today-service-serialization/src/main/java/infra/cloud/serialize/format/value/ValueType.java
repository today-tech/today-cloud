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

import infra.cloud.serialize.format.MessageFormat;

/**
 * Representation of MessagePack types.
 * <p>
 * MessagePack uses hierarchical type system. Integer and Float are subypte of Number, Thus {@link #isNumberType()}
 * returns true if type is Integer or Float. String and Binary are subtype of Raw. Thus {@link #isRawType()} returns
 * true if type is String or Binary.
 *
 * @see MessageFormat
 */
public enum ValueType {
  NIL(false, false),
  BOOLEAN(false, false),
  INTEGER(true, false),
  FLOAT(true, false),
  STRING(false, true),
  BINARY(false, true),
  ARRAY(false, false),
  MAP(false, false),
  EXTENSION(false, false);

  /**
   * Design note: We do not add Timestamp as a ValueType here because
   * detecting Timestamp values requires reading 1-3 bytes ahead while the other
   * value types can be determined just by reading the first one byte.
   */

  private final boolean numberType;
  private final boolean rawType;

  private ValueType(boolean numberType, boolean rawType) {
    this.numberType = numberType;
    this.rawType = rawType;
  }

  public boolean isNilType() {
    return this == NIL;
  }

  public boolean isBooleanType() {
    return this == BOOLEAN;
  }

  public boolean isNumberType() {
    return numberType;
  }

  public boolean isIntegerType() {
    return this == INTEGER;
  }

  public boolean isFloatType() {
    return this == FLOAT;
  }

  public boolean isRawType() {
    return rawType;
  }

  public boolean isStringType() {
    return this == STRING;
  }

  public boolean isBinaryType() {
    return this == BINARY;
  }

  public boolean isArrayType() {
    return this == ARRAY;
  }

  public boolean isMapType() {
    return this == MAP;
  }

  public boolean isExtensionType() {
    return this == EXTENSION;
  }
}
