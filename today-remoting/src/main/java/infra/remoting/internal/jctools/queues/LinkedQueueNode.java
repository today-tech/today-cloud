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

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package infra.remoting.internal.jctools.queues;

import static infra.remoting.internal.jctools.queues.UnsafeAccess.UNSAFE;
import static infra.remoting.internal.jctools.queues.UnsafeAccess.fieldOffset;

final class LinkedQueueNode<E> {
  private static final long NEXT_OFFSET = fieldOffset(LinkedQueueNode.class, "next");

  private E value;
  private volatile LinkedQueueNode<E> next;

  LinkedQueueNode() {
    this(null);
  }

  LinkedQueueNode(E val) {
    spValue(val);
  }

  /**
   * Gets the current value and nulls out the reference to it from this node.
   *
   * @return value
   */
  public E getAndNullValue() {
    E temp = lpValue();
    spValue(null);
    return temp;
  }

  public E lpValue() {
    return value;
  }

  public void spValue(E newValue) {
    value = newValue;
  }

  public void soNext(LinkedQueueNode<E> n) {
    UNSAFE.putOrderedObject(this, NEXT_OFFSET, n);
  }

  public void spNext(LinkedQueueNode<E> n) {
    UNSAFE.putObject(this, NEXT_OFFSET, n);
  }

  public LinkedQueueNode<E> lvNext() {
    return next;
  }
}
