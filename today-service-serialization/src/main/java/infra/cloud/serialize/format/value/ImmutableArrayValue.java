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
 * Immutable representation of MessagePack's Array type.
 *
 * MessagePack's Array type can represent sequence of values.
 */
public interface ImmutableArrayValue
        extends ArrayValue, ImmutableValue {
  /**
   * Returns an iterator over elements.
   * Returned Iterator does not support {@code remove()} method since the value is immutable.
   */
  Iterator<Value> iterator();

  /**
   * Returns the value as {@code List}.
   * Returned List is immutable. It does not support {@code put()}, {@code clear()}, or other methods that modify the value.
   */
  List<Value> list();
}
