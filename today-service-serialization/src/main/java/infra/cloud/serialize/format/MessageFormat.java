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

package infra.cloud.serialize.format;

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
    if (MessagePackCode.isPosFixInt(b)) {
      return POS_FIX_INT;
    }
    if (MessagePackCode.isNegFixInt(b)) {
      return NEG_FIX_INT;
    }
    if (MessagePackCode.isFixStr(b)) {
      return FIX_STR;
    }
    if (MessagePackCode.isFixedArray(b)) {
      return FIX_ARRAY;
    }
    if (MessagePackCode.isFixedMap(b)) {
      return FIX_MAP;
    }
    return switch (b) {
      case MessagePackCode.NIL -> NIL;
      case MessagePackCode.FALSE, MessagePackCode.TRUE -> BOOLEAN;
      case MessagePackCode.BIN8 -> BIN8;
      case MessagePackCode.BIN16 -> BIN16;
      case MessagePackCode.BIN32 -> BIN32;
      case MessagePackCode.EXT8 -> EXT8;
      case MessagePackCode.EXT16 -> EXT16;
      case MessagePackCode.EXT32 -> EXT32;
      case MessagePackCode.FLOAT32 -> FLOAT32;
      case MessagePackCode.FLOAT64 -> FLOAT64;
      case MessagePackCode.UINT8 -> UINT8;
      case MessagePackCode.UINT16 -> UINT16;
      case MessagePackCode.UINT32 -> UINT32;
      case MessagePackCode.UINT64 -> UINT64;
      case MessagePackCode.INT8 -> INT8;
      case MessagePackCode.INT16 -> INT16;
      case MessagePackCode.INT32 -> INT32;
      case MessagePackCode.INT64 -> INT64;
      case MessagePackCode.FIXEXT1 -> FIX_EXT1;
      case MessagePackCode.FIXEXT2 -> FIX_EXT2;
      case MessagePackCode.FIXEXT4 -> FIX_EXT4;
      case MessagePackCode.FIXEXT8 -> FIX_EXT8;
      case MessagePackCode.FIXEXT16 -> FIX_EXT16;
      case MessagePackCode.STR8 -> STR8;
      case MessagePackCode.STR16 -> STR16;
      case MessagePackCode.STR32 -> STR32;
      case MessagePackCode.ARRAY16 -> ARRAY16;
      case MessagePackCode.ARRAY32 -> ARRAY32;
      case MessagePackCode.MAP16 -> MAP16;
      case MessagePackCode.MAP32 -> MAP32;
      default -> NEVER_USED;
    };
  }
}
