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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Representation of MessagePack's Map type.
 * <p>
 * MessagePack's Map type can represent sequence of key-value pairs.
 */
public interface MapValue extends Value {

  /**
   * Returns number of key-value pairs in this array.
   */
  int size();

  Set<Value> keySet();

  Set<Map.Entry<Value, Value>> entrySet();

  Collection<Value> values();

  /**
   * Returns the value as {@code Map}.
   */
  Map<Value, Value> map();

  /**
   * Returns the key-value pairs as an array of {@code Value}.
   * <p>
   * Odd elements are keys. Next element of an odd element is a value corresponding to the key.
   * <p>
   * For example, if this value represents <code>{"k1": "v1", "k2": "v2"}</code>, this method returns ["k1", "v1", "k2", "v2"].
   */
  Value[] getKeyValueArray();

}
