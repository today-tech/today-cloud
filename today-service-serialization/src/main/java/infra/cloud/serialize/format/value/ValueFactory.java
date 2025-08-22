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
import java.time.Instant;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import infra.cloud.serialize.format.value.impl.ImmutableArrayValueImpl;
import infra.cloud.serialize.format.value.impl.ImmutableBigIntegerValueImpl;
import infra.cloud.serialize.format.value.impl.ImmutableBinaryValueImpl;
import infra.cloud.serialize.format.value.impl.ImmutableBooleanValueImpl;
import infra.cloud.serialize.format.value.impl.ImmutableDoubleValueImpl;
import infra.cloud.serialize.format.value.impl.ImmutableExtensionValueImpl;
import infra.cloud.serialize.format.value.impl.ImmutableLongValueImpl;
import infra.cloud.serialize.format.value.impl.ImmutableMapValueImpl;
import infra.cloud.serialize.format.value.impl.ImmutableNilValueImpl;
import infra.cloud.serialize.format.value.impl.ImmutableStringValueImpl;
import infra.cloud.serialize.format.value.impl.ImmutableTimestampValueImpl;

public final class ValueFactory {
  private ValueFactory() {
  }

  public static ImmutableNilValue newNil() {
    return ImmutableNilValueImpl.get();
  }

  public static ImmutableBooleanValue newBoolean(boolean v) {
    return v ? ImmutableBooleanValueImpl.TRUE : ImmutableBooleanValueImpl.FALSE;
  }

  public static ImmutableIntegerValue newInteger(byte v) {
    return new ImmutableLongValueImpl(v);
  }

  public static ImmutableIntegerValue newInteger(short v) {
    return new ImmutableLongValueImpl(v);
  }

  public static ImmutableIntegerValue newInteger(int v) {
    return new ImmutableLongValueImpl(v);
  }

  public static ImmutableIntegerValue newInteger(long v) {
    return new ImmutableLongValueImpl(v);
  }

  public static ImmutableIntegerValue newInteger(BigInteger v) {
    return new ImmutableBigIntegerValueImpl(v);
  }

  public static ImmutableFloatValue newFloat(float v) {
    return new ImmutableDoubleValueImpl(v);
  }

  public static ImmutableFloatValue newFloat(double v) {
    return new ImmutableDoubleValueImpl(v);
  }

  public static ImmutableBinaryValue newBinary(byte[] b) {
    return newBinary(b, false);
  }

  public static ImmutableBinaryValue newBinary(byte[] b, boolean omitCopy) {
    if (omitCopy) {
      return new ImmutableBinaryValueImpl(b);
    }
    else {
      return new ImmutableBinaryValueImpl(Arrays.copyOf(b, b.length));
    }
  }

  public static ImmutableBinaryValue newBinary(byte[] b, int off, int len) {
    return newBinary(b, off, len, false);
  }

  public static ImmutableBinaryValue newBinary(byte[] b, int off, int len, boolean omitCopy) {
    if (omitCopy && off == 0 && len == b.length) {
      return new ImmutableBinaryValueImpl(b);
    }
    else {
      return new ImmutableBinaryValueImpl(Arrays.copyOfRange(b, off, len));
    }
  }

  public static ImmutableStringValue newString(String s) {
    return new ImmutableStringValueImpl(s);
  }

  public static ImmutableStringValue newString(byte[] b) {
    return new ImmutableStringValueImpl(b);
  }

  public static ImmutableStringValue newString(byte[] b, boolean omitCopy) {
    if (omitCopy) {
      return new ImmutableStringValueImpl(b);
    }
    else {
      return new ImmutableStringValueImpl(Arrays.copyOf(b, b.length));
    }
  }

  public static ImmutableStringValue newString(byte[] b, int off, int len) {
    return newString(b, off, len, false);
  }

  public static ImmutableStringValue newString(byte[] b, int off, int len, boolean omitCopy) {
    if (omitCopy && off == 0 && len == b.length) {
      return new ImmutableStringValueImpl(b);
    }
    else {
      return new ImmutableStringValueImpl(Arrays.copyOfRange(b, off, len));
    }
  }

  public static ImmutableArrayValue newArray(List<? extends Value> list) {
    if (list.isEmpty()) {
      return ImmutableArrayValueImpl.empty();
    }
    Value[] array = list.toArray(new Value[list.size()]);
    return new ImmutableArrayValueImpl(array);
  }

  public static ImmutableArrayValue newArray(Value... array) {
    if (array.length == 0) {
      return ImmutableArrayValueImpl.empty();
    }
    else {
      return new ImmutableArrayValueImpl(Arrays.copyOf(array, array.length));
    }
  }

  public static ImmutableArrayValue newArray(Value[] array, boolean omitCopy) {
    if (array.length == 0) {
      return ImmutableArrayValueImpl.empty();
    }
    else if (omitCopy) {
      return new ImmutableArrayValueImpl(array);
    }
    else {
      return new ImmutableArrayValueImpl(Arrays.copyOf(array, array.length));
    }
  }

  public static ImmutableArrayValue emptyArray() {
    return ImmutableArrayValueImpl.empty();
  }

  public static <K extends Value, V extends Value>
  ImmutableMapValue newMap(Map<K, V> map) {
    Value[] kvs = new Value[map.size() * 2];
    int index = 0;
    for (Map.Entry<K, V> pair : map.entrySet()) {
      kvs[index] = pair.getKey();
      index++;
      kvs[index] = pair.getValue();
      index++;
    }
    return new ImmutableMapValueImpl(kvs);
  }

  public static ImmutableMapValue newMap(Value... kvs) {
    if (kvs.length == 0) {
      return ImmutableMapValueImpl.empty();
    }
    else {
      return new ImmutableMapValueImpl(Arrays.copyOf(kvs, kvs.length));
    }
  }

  public static ImmutableMapValue newMap(Value[] kvs, boolean omitCopy) {
    if (kvs.length == 0) {
      return ImmutableMapValueImpl.empty();
    }
    else if (omitCopy) {
      return new ImmutableMapValueImpl(kvs);
    }
    else {
      return new ImmutableMapValueImpl(Arrays.copyOf(kvs, kvs.length));
    }
  }

  public static ImmutableMapValue emptyMap() {
    return ImmutableMapValueImpl.empty();
  }

  @SafeVarargs
  public static MapValue newMap(Map.Entry<? extends Value, ? extends Value>... pairs) {
    Value[] kvs = new Value[pairs.length * 2];
    for (int i = 0; i < pairs.length; ++i) {
      kvs[i * 2] = pairs[i].getKey();
      kvs[i * 2 + 1] = pairs[i].getValue();
    }
    return newMap(kvs, true);
  }

  public static MapBuilder newMapBuilder() {
    return new MapBuilder();
  }

  public static Map.Entry<Value, Value> newMapEntry(Value key, Value value) {
    return new AbstractMap.SimpleEntry<>(key, value);
  }

  public static class MapBuilder {
    private final Map<Value, Value> map = new LinkedHashMap<Value, Value>();

    public MapBuilder() {
    }

    public MapValue build() {
      return newMap(map);
    }

    public MapBuilder put(Map.Entry<? extends Value, ? extends Value> pair) {
      put(pair.getKey(), pair.getValue());
      return this;
    }

    public MapBuilder put(Value key, Value value) {
      map.put(key, value);
      return this;
    }

    public MapBuilder putAll(Iterable<? extends Map.Entry<? extends Value, ? extends Value>> entries) {
      for (Map.Entry<? extends Value, ? extends Value> entry : entries) {
        put(entry.getKey(), entry.getValue());
      }
      return this;
    }

    public MapBuilder putAll(Map<? extends Value, ? extends Value> map) {
      for (Map.Entry<? extends Value, ? extends Value> entry : map.entrySet()) {
        put(entry);
      }
      return this;
    }
  }

  public static ImmutableExtensionValue newExtension(byte type, byte[] data) {
    return new ImmutableExtensionValueImpl(type, data);
  }

  public static ImmutableTimestampValue newTimestamp(Instant timestamp) {
    return new ImmutableTimestampValueImpl(timestamp);
  }

  public static ImmutableTimestampValue newTimestamp(long millis) {
    return newTimestamp(Instant.ofEpochMilli(millis));
  }

  public static ImmutableTimestampValue newTimestamp(long epochSecond, int nanoAdjustment) {
    return newTimestamp(Instant.ofEpochSecond(epochSecond, nanoAdjustment));
  }
}
