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
import java.util.Arrays;

import infra.cloud.serialize.format.MessagePacker;
import infra.cloud.serialize.format.value.ImmutableStringValue;
import infra.cloud.serialize.format.value.StringValue;
import infra.cloud.serialize.format.value.Value;
import infra.cloud.serialize.format.value.ValueType;

/**
 * {@code ImmutableStringValueImpl} Implements {@code ImmutableStringValue} using a {@code byte[]} field.
 * This implementation caches result of {@code toString()} and {@code asString()} using a private {@code String} field.
 *
 * @see StringValue
 */
public class ImmutableStringValueImpl
        extends AbstractImmutableRawValue
        implements ImmutableStringValue {
  public ImmutableStringValueImpl(byte[] data) {
    super(data);
  }

  public ImmutableStringValueImpl(String string) {
    super(string);
  }

  @Override
  public ValueType getValueType() {
    return ValueType.STRING;
  }

  @Override
  public ImmutableStringValue immutableValue() {
    return this;
  }

  @Override
  public ImmutableStringValue asStringValue() {
    return this;
  }

  @Override
  public void writeTo(MessagePacker pk)
          throws IOException {
    pk.packRawStringHeader(data.length);
    pk.writePayload(data);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Value)) {
      return false;
    }
    Value v = (Value) o;
    if (!v.isStringValue()) {
      return false;
    }

    if (v instanceof ImmutableStringValueImpl) {
      ImmutableStringValueImpl bv = (ImmutableStringValueImpl) v;
      return Arrays.equals(data, bv.data);
    }
    else {
      return Arrays.equals(data, v.asStringValue().asByteArray());
    }
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(data);
  }
}
