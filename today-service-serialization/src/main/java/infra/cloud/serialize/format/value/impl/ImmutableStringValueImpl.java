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
