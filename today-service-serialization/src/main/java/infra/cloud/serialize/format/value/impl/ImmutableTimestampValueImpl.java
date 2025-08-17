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
import java.time.Instant;
import java.util.Arrays;

import infra.cloud.serialize.format.MessagePacker;
import infra.cloud.serialize.format.buffer.MessageBuffer;
import infra.cloud.serialize.format.value.ExtensionValue;
import infra.cloud.serialize.format.value.ImmutableExtensionValue;
import infra.cloud.serialize.format.value.ImmutableTimestampValue;
import infra.cloud.serialize.format.value.TimestampValue;
import infra.cloud.serialize.format.value.Value;
import infra.cloud.serialize.format.value.ValueType;

import static infra.cloud.serialize.format.MessagePack.Code.EXT_TIMESTAMP;

/**
 * {@code ImmutableTimestampValueImpl} Implements {@code ImmutableTimestampValue} using a {@code byte} and a {@code byte[]} fields.
 *
 * @see TimestampValue
 */
public class ImmutableTimestampValueImpl
        extends AbstractImmutableValue
        implements ImmutableExtensionValue, ImmutableTimestampValue {
  private final Instant instant;
  private byte[] data;

  public ImmutableTimestampValueImpl(Instant timestamp) {
    this.instant = timestamp;
  }

  @Override
  public boolean isTimestampValue() {
    return true;
  }

  @Override
  public byte getType() {
    return EXT_TIMESTAMP;
  }

  @Override
  public ValueType getValueType() {
    // Note: Future version should return ValueType.TIMESTAMP instead.
    return ValueType.EXTENSION;
  }

  @Override
  public ImmutableTimestampValue immutableValue() {
    return this;
  }

  @Override
  public ImmutableExtensionValue asExtensionValue() {
    return this;
  }

  @Override
  public ImmutableTimestampValue asTimestampValue() {
    return this;
  }

  @Override
  public byte[] getData() {
    if (data == null) {
      // See MessagePacker.packTimestampImpl
      byte[] bytes;
      long sec = getEpochSecond();
      int nsec = getNano();
      if (sec >>> 34 == 0) {
        long data64 = ((long) nsec << 34) | sec;
        if ((data64 & 0xffffffff00000000L) == 0L) {
          bytes = new byte[4];
          MessageBuffer.wrap(bytes).putInt(0, (int) sec);
        }
        else {
          bytes = new byte[8];
          MessageBuffer.wrap(bytes).putLong(0, data64);
        }
      }
      else {
        bytes = new byte[12];
        MessageBuffer buffer = MessageBuffer.wrap(bytes);
        buffer.putInt(0, nsec);
        buffer.putLong(4, sec);
      }
      data = bytes;
    }
    return data;
  }

  @Override
  public long getEpochSecond() {
    return instant.getEpochSecond();
  }

  @Override
  public int getNano() {
    return instant.getNano();
  }

  @Override
  public long toEpochMillis() {
    return instant.toEpochMilli();
  }

  @Override
  public Instant toInstant() {
    return instant;
  }

  @Override
  public void writeTo(MessagePacker packer)
          throws IOException {
    packer.packTimestamp(instant);
  }

  @Override
  public boolean equals(Object o) {
    // Implements same behavior with ImmutableExtensionValueImpl.
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

    // Here should use isTimestampValue and asTimestampValue instead. However, because
    // adding these methods to Value interface can't keep backward compatibility without
    // using "default" keyword since Java 7, here uses instanceof of and cast instead.
    if (ev instanceof TimestampValue) {
      TimestampValue tv = (TimestampValue) ev;
      return instant.equals(tv.toInstant());
    }
    else {
      return EXT_TIMESTAMP == ev.getType() && Arrays.equals(getData(), ev.getData());
    }
  }

  @Override
  public int hashCode() {
    // Implements same behavior with ImmutableExtensionValueImpl.
    int hash = EXT_TIMESTAMP;
    hash *= 31;
    hash = instant.hashCode();
    return hash;
  }

  @Override
  public String toJson() {
    return "\"" + toInstant().toString() + "\"";
  }

  @Override
  public String toString() {
    return toInstant().toString();
  }
}
