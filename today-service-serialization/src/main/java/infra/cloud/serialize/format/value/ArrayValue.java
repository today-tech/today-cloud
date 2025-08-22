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

import java.util.Iterator;
import java.util.List;

/**
 * Representation of MessagePack's Array type.
 * <p>
 * MessagePack's Array type can represent sequence of values.
 */
public interface ArrayValue extends Value, Iterable<Value> {

  /**
   * Returns number of elements in this array.
   */
  int size();

  /**
   * Returns the element at the specified position in this array.
   *
   * @throws IndexOutOfBoundsException If the index is out of range
   * (<tt>index &lt; 0 || index &gt;= size()</tt>)
   */
  Value get(int index);

  /**
   * Returns the element at the specified position in this array.
   * This method returns an ImmutableNilValue if the index is out of range.
   */
  Value getOrNilValue(int index);

  /**
   * Returns an iterator over elements.
   */
  Iterator<Value> iterator();

  /**
   * Returns the value as {@code List}.
   */
  List<Value> list();
}
