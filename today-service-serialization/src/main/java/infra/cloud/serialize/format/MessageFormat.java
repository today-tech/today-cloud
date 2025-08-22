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

import infra.cloud.serialize.format.MessagePack.Code;
import infra.cloud.serialize.format.value.ValueType;
import infra.lang.VisibleForTesting;

/**
 * Describes the list of the message format types defined in the MessagePack specification.
 */
public enum MessageFormat {
  // INT7
  POS_FIX_INT(ValueType.INTEGER),
  // MAP4
  FIX_MAP(ValueType.MAP),
  // ARRAY4
  FIX_ARRAY(ValueType.ARRAY),
  // STR5
  FIX_STR(ValueType.STRING),
  NIL(ValueType.NIL),
  NEVER_USED(null),
  BOOLEAN(ValueType.BOOLEAN),
  BIN8(ValueType.BINARY),
  BIN16(ValueType.BINARY),
  BIN32(ValueType.BINARY),
  EXT8(ValueType.EXTENSION),
  EXT16(ValueType.EXTENSION),
  EXT32(ValueType.EXTENSION),
  FLOAT32(ValueType.FLOAT),
  FLOAT64(ValueType.FLOAT),
  UINT8(ValueType.INTEGER),
  UINT16(ValueType.INTEGER),
  UINT32(ValueType.INTEGER),
  UINT64(ValueType.INTEGER),

  INT8(ValueType.INTEGER),
  INT16(ValueType.INTEGER),
  INT32(ValueType.INTEGER),
  INT64(ValueType.INTEGER),
  FIX_EXT1(ValueType.EXTENSION),
  FIX_EXT2(ValueType.EXTENSION),
  FIX_EXT4(ValueType.EXTENSION),
  FIX_EXT8(ValueType.EXTENSION),
  FIX_EXT16(ValueType.EXTENSION),
  STR8(ValueType.STRING),
  STR16(ValueType.STRING),
  STR32(ValueType.STRING),
  ARRAY16(ValueType.ARRAY),
  ARRAY32(ValueType.ARRAY),
  MAP16(ValueType.MAP),
  MAP32(ValueType.MAP),
  NEG_FIX_INT(ValueType.INTEGER);

  private static final MessageFormat[] formatTable = new MessageFormat[256];

  private final ValueType valueType;

  MessageFormat(ValueType valueType) {
    this.valueType = valueType;
  }

  /**
   * Retruns the ValueType corresponding to this MessageFormat
   *
   * @return value type
   * @throws MessageFormatException if this == NEVER_USED type
   */
  public ValueType getValueType() throws MessageFormatException {
    if (this == NEVER_USED) {
      throw new MessageFormatException("Cannot convert NEVER_USED to ValueType");
    }
    return valueType;
  }

  static {
    // Preparing a look up table for converting byte values into MessageFormat types
    for (int b = 0; b <= 0xFF; ++b) {
      MessageFormat mf = toMessageFormat((byte) b);
      formatTable[b] = mf;
    }
  }

  /**
   * Returns a MessageFormat type of the specified byte value
   *
   * @param b MessageFormat of the given byte
   */
  public static MessageFormat valueOf(final byte b) {
    return formatTable[b & 0xFF];
  }

  /**
   * Converting a byte value into MessageFormat. For faster performance, use {@link #valueOf}
   *
   * @param b MessageFormat of the given byte
   */
  @VisibleForTesting
  static MessageFormat toMessageFormat(final byte b) {
    if (Code.isPosFixInt(b)) {
      return POS_FIX_INT;
    }
    if (Code.isNegFixInt(b)) {
      return NEG_FIX_INT;
    }
    if (Code.isFixStr(b)) {
      return FIX_STR;
    }
    if (Code.isFixedArray(b)) {
      return FIX_ARRAY;
    }
    if (Code.isFixedMap(b)) {
      return FIX_MAP;
    }
    return switch (b) {
      case Code.NIL -> NIL;
      case Code.FALSE, Code.TRUE -> BOOLEAN;
      case Code.BIN8 -> BIN8;
      case Code.BIN16 -> BIN16;
      case Code.BIN32 -> BIN32;
      case Code.EXT8 -> EXT8;
      case Code.EXT16 -> EXT16;
      case Code.EXT32 -> EXT32;
      case Code.FLOAT32 -> FLOAT32;
      case Code.FLOAT64 -> FLOAT64;
      case Code.UINT8 -> UINT8;
      case Code.UINT16 -> UINT16;
      case Code.UINT32 -> UINT32;
      case Code.UINT64 -> UINT64;
      case Code.INT8 -> INT8;
      case Code.INT16 -> INT16;
      case Code.INT32 -> INT32;
      case Code.INT64 -> INT64;
      case Code.FIXEXT1 -> FIX_EXT1;
      case Code.FIXEXT2 -> FIX_EXT2;
      case Code.FIXEXT4 -> FIX_EXT4;
      case Code.FIXEXT8 -> FIX_EXT8;
      case Code.FIXEXT16 -> FIX_EXT16;
      case Code.STR8 -> STR8;
      case Code.STR16 -> STR16;
      case Code.STR32 -> STR32;
      case Code.ARRAY16 -> ARRAY16;
      case Code.ARRAY32 -> ARRAY32;
      case Code.MAP16 -> MAP16;
      case Code.MAP32 -> MAP32;
      default -> NEVER_USED;
    };
  }
}
