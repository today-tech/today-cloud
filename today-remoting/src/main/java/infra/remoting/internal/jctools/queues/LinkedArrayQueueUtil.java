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

import static infra.remoting.internal.jctools.queues.UnsafeRefArrayAccess.REF_ARRAY_BASE;
import static infra.remoting.internal.jctools.queues.UnsafeRefArrayAccess.REF_ELEMENT_SHIFT;

/** This is used for method substitution in the LinkedArray classes code generation. */
final class LinkedArrayQueueUtil {
  static int length(Object[] buf) {
    return buf.length;
  }

  /**
   * This method assumes index is actually (index << 1) because lower bit is used for resize. This
   * is compensated for by reducing the element shift. The computation is constant folded, so
   * there's no cost.
   */
  static long modifiedCalcCircularRefElementOffset(long index, long mask) {
    return REF_ARRAY_BASE + ((index & mask) << (REF_ELEMENT_SHIFT - 1));
  }

  static long nextArrayOffset(Object[] curr) {
    return REF_ARRAY_BASE + ((long) (length(curr) - 1) << REF_ELEMENT_SHIFT);
  }
}
