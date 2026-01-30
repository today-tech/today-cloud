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
