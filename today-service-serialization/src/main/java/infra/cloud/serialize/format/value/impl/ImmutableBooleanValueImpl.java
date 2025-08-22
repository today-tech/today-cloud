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

import infra.cloud.serialize.format.MessagePacker;
import infra.cloud.serialize.format.value.BooleanValue;
import infra.cloud.serialize.format.value.ImmutableBooleanValue;
import infra.cloud.serialize.format.value.Value;
import infra.cloud.serialize.format.value.ValueType;

/**
 * {@code ImmutableBooleanValueImpl} Implements {@code ImmutableBooleanValue} using a {@code boolean} field.
 *
 * This class is a singleton. {@code ImmutableBooleanValueImpl.trueInstance()} and {@code ImmutableBooleanValueImpl.falseInstance()} are the only instances of this class.
 *
 * @see BooleanValue
 */
public class ImmutableBooleanValueImpl
        extends AbstractImmutableValue
        implements ImmutableBooleanValue {
  public static final ImmutableBooleanValue TRUE = new ImmutableBooleanValueImpl(true);
  public static final ImmutableBooleanValue FALSE = new ImmutableBooleanValueImpl(false);

  private final boolean value;

  private ImmutableBooleanValueImpl(boolean value) {
    this.value = value;
  }

  @Override
  public ValueType getValueType() {
    return ValueType.BOOLEAN;
  }

  @Override
  public ImmutableBooleanValue asBooleanValue() {
    return this;
  }

  @Override
  public ImmutableBooleanValue immutableValue() {
    return this;
  }

  @Override
  public boolean getBoolean() {
    return value;
  }

  @Override
  public void writeTo(MessagePacker packer)
          throws IOException {
    packer.packBoolean(value);
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

    if (!v.isBooleanValue()) {
      return false;
    }
    return value == v.asBooleanValue().getBoolean();
  }

  @Override
  public int hashCode() {
    if (value) {
      return 1231;
    }
    else {
      return 1237;
    }
  }

  @Override
  public String toJson() {
    return Boolean.toString(value);
  }

  @Override
  public String toString() {
    return toJson();
  }
}
