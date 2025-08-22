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

package infra.cloud.serialize.format.value.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import infra.cloud.serialize.format.MessagePacker;
import infra.cloud.serialize.format.value.FloatValue;
import infra.cloud.serialize.format.value.ImmutableFloatValue;
import infra.cloud.serialize.format.value.ImmutableNumberValue;
import infra.cloud.serialize.format.value.Value;
import infra.cloud.serialize.format.value.ValueType;

/**
 * {@code ImmutableDoubleValueImpl} Implements {@code ImmutableFloatValue} using a {@code double} field.
 *
 * @see FloatValue
 */
public class ImmutableDoubleValueImpl extends AbstractImmutableValue implements ImmutableFloatValue {

  private final double value;

  public ImmutableDoubleValueImpl(double value) {
    this.value = value;
  }

  @Override
  public ValueType getValueType() {
    return ValueType.FLOAT;
  }

  @Override
  public ImmutableDoubleValueImpl immutableValue() {
    return this;
  }

  @Override
  public ImmutableNumberValue asNumberValue() {
    return this;
  }

  @Override
  public ImmutableFloatValue asFloatValue() {
    return this;
  }

  @Override
  public byte toByte() {
    return (byte) value;
  }

  @Override
  public short toShort() {
    return (short) value;
  }

  @Override
  public int toInt() {
    return (int) value;
  }

  @Override
  public long toLong() {
    return (long) value;
  }

  @Override
  public BigInteger toBigInteger() {
    return new BigDecimal(value).toBigInteger();
  }

  @Override
  public float toFloat() {
    return (float) value;
  }

  @Override
  public double toDouble() {
    return value;
  }

  @Override
  public void writeTo(MessagePacker pk)
          throws IOException {
    pk.packDouble(value);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof Value)) {
      return false;
    }
    Value v = (Value) o;

    if (!v.isFloatValue()) {
      return false;
    }
    return value == v.asFloatValue().toDouble();
  }

  @Override
  public int hashCode() {
    long v = Double.doubleToLongBits(value);
    return (int) (v ^ (v >>> 32));
  }

  @Override
  public String toJson() {
    if (Double.isNaN(value) || Double.isInfinite(value)) {
      return "null";
    }
    else {
      return Double.toString(value);
    }
  }

  @Override
  public String toString() {
    return Double.toString(value);
  }
}
