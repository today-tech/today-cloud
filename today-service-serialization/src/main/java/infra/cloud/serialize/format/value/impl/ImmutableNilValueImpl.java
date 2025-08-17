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
import infra.cloud.serialize.format.value.ImmutableNilValue;
import infra.cloud.serialize.format.value.NilValue;
import infra.cloud.serialize.format.value.Value;
import infra.cloud.serialize.format.value.ValueType;

/**
 * {@code ImmutableNilValueImpl} Implements {@code ImmutableNilValue}.
 * <p>
 * This class is a singleton. {@code ImmutableNilValueImpl.get()} is the only instances of this class.
 *
 * @see NilValue
 */
public class ImmutableNilValueImpl extends AbstractImmutableValue implements ImmutableNilValue {

  private static ImmutableNilValue instance = new ImmutableNilValueImpl();

  public static ImmutableNilValue get() {
    return instance;
  }

  private ImmutableNilValueImpl() {
  }

  @Override
  public ValueType getValueType() {
    return ValueType.NIL;
  }

  @Override
  public ImmutableNilValue immutableValue() {
    return this;
  }

  @Override
  public ImmutableNilValue asNilValue() {
    return this;
  }

  @Override
  public void writeTo(MessagePacker pk)
          throws IOException {
    pk.packNil();
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof Value)) {
      return false;
    }
    return ((Value) o).isNilValue();
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public String toString() {
    return toJson();
  }

  @Override
  public String toJson() {
    return "null";
  }
}
