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

import infra.cloud.serialize.format.MessageTypeCastException;
import infra.cloud.serialize.format.value.ImmutableArrayValue;
import infra.cloud.serialize.format.value.ImmutableBinaryValue;
import infra.cloud.serialize.format.value.ImmutableBooleanValue;
import infra.cloud.serialize.format.value.ImmutableExtensionValue;
import infra.cloud.serialize.format.value.ImmutableFloatValue;
import infra.cloud.serialize.format.value.ImmutableIntegerValue;
import infra.cloud.serialize.format.value.ImmutableMapValue;
import infra.cloud.serialize.format.value.ImmutableNilValue;
import infra.cloud.serialize.format.value.ImmutableNumberValue;
import infra.cloud.serialize.format.value.ImmutableRawValue;
import infra.cloud.serialize.format.value.ImmutableStringValue;
import infra.cloud.serialize.format.value.ImmutableTimestampValue;
import infra.cloud.serialize.format.value.ImmutableValue;

abstract class AbstractImmutableValue implements ImmutableValue {

  @Override
  public boolean isNilValue() {
    return getValueType().isNilType();
  }

  @Override
  public boolean isBooleanValue() {
    return getValueType().isBooleanType();
  }

  @Override
  public boolean isNumberValue() {
    return getValueType().isNumberType();
  }

  @Override
  public boolean isIntegerValue() {
    return getValueType().isIntegerType();
  }

  @Override
  public boolean isFloatValue() {
    return getValueType().isFloatType();
  }

  @Override
  public boolean isRawValue() {
    return getValueType().isRawType();
  }

  @Override
  public boolean isBinaryValue() {
    return getValueType().isBinaryType();
  }

  @Override
  public boolean isStringValue() {
    return getValueType().isStringType();
  }

  @Override
  public boolean isArrayValue() {
    return getValueType().isArrayType();
  }

  @Override
  public boolean isMapValue() {
    return getValueType().isMapType();
  }

  @Override
  public boolean isExtensionValue() {
    return getValueType().isExtensionType();
  }

  @Override
  public boolean isTimestampValue() {
    return false;
  }

  @Override
  public ImmutableNilValue asNilValue() {
    throw new MessageTypeCastException();
  }

  @Override
  public ImmutableBooleanValue asBooleanValue() {
    throw new MessageTypeCastException();
  }

  @Override
  public ImmutableNumberValue asNumberValue() {
    throw new MessageTypeCastException();
  }

  @Override
  public ImmutableIntegerValue asIntegerValue() {
    throw new MessageTypeCastException();
  }

  @Override
  public ImmutableFloatValue asFloatValue() {
    throw new MessageTypeCastException();
  }

  @Override
  public ImmutableRawValue asRawValue() {
    throw new MessageTypeCastException();
  }

  @Override
  public ImmutableBinaryValue asBinaryValue() {
    throw new MessageTypeCastException();
  }

  @Override
  public ImmutableStringValue asStringValue() {
    throw new MessageTypeCastException();
  }

  @Override
  public ImmutableArrayValue asArrayValue() {
    throw new MessageTypeCastException();
  }

  @Override
  public ImmutableMapValue asMapValue() {
    throw new MessageTypeCastException();
  }

  @Override
  public ImmutableExtensionValue asExtensionValue() {
    throw new MessageTypeCastException();
  }

  @Override
  public ImmutableTimestampValue asTimestampValue() {
    throw new MessageTypeCastException();
  }
}
