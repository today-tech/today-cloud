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
import infra.cloud.serialize.format.value.ExtensionValue;
import infra.cloud.serialize.format.value.ImmutableExtensionValue;
import infra.cloud.serialize.format.value.Value;
import infra.cloud.serialize.format.value.ValueType;

/**
 * {@code ImmutableExtensionValueImpl} Implements {@code ImmutableExtensionValue} using a {@code byte} and a {@code byte[]} fields.
 *
 * @see ExtensionValue
 */
public class ImmutableExtensionValueImpl
        extends AbstractImmutableValue
        implements ImmutableExtensionValue {
  private final byte type;
  private final byte[] data;

  public ImmutableExtensionValueImpl(byte type, byte[] data) {
    this.type = type;
    this.data = data;
  }

  @Override
  public ValueType getValueType() {
    return ValueType.EXTENSION;
  }

  @Override
  public ImmutableExtensionValue immutableValue() {
    return this;
  }

  @Override
  public ImmutableExtensionValue asExtensionValue() {
    return this;
  }

  @Override
  public byte getType() {
    return type;
  }

  @Override
  public byte[] getData() {
    return data;
  }

  @Override
  public void writeTo(MessagePacker packer)
          throws IOException {
    packer.packExtensionTypeHeader(type, data.length);
    packer.writePayload(data);
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

    if (!v.isExtensionValue()) {
      return false;
    }
    ExtensionValue ev = v.asExtensionValue();
    return type == ev.getType() && Arrays.equals(data, ev.getData());
  }

  @Override
  public int hashCode() {
    int hash = 31 + type;
    for (byte e : data) {
      hash = 31 * hash + e;
    }
    return hash;
  }

  @Override
  public String toJson() {
    StringBuilder sb = new StringBuilder();
    sb.append('[');
    sb.append(Byte.toString(type));
    sb.append(",\"");
    for (byte e : data) {
      sb.append(Integer.toString((int) e, 16));
    }
    sb.append("\"]");
    return sb.toString();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append('(');
    sb.append(Byte.toString(type));
    sb.append(",0x");
    for (byte e : data) {
      sb.append(Integer.toString((int) e, 16));
    }
    sb.append(")");
    return sb.toString();
  }
}
