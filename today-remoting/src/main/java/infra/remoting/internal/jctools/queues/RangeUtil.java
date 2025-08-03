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

final class RangeUtil {
  public static long checkPositive(long n, String name) {
    if (n <= 0) {
      throw new IllegalArgumentException(name + ": " + n + " (expected: > 0)");
    }

    return n;
  }

  public static int checkPositiveOrZero(int n, String name) {
    if (n < 0) {
      throw new IllegalArgumentException(name + ": " + n + " (expected: >= 0)");
    }

    return n;
  }

  public static int checkLessThan(int n, int expected, String name) {
    if (n >= expected) {
      throw new IllegalArgumentException(name + ": " + n + " (expected: < " + expected + ')');
    }

    return n;
  }

  public static int checkLessThanOrEqual(int n, long expected, String name) {
    if (n > expected) {
      throw new IllegalArgumentException(name + ": " + n + " (expected: <= " + expected + ')');
    }

    return n;
  }

  public static int checkGreaterThanOrEqual(int n, int expected, String name) {
    if (n < expected) {
      throw new IllegalArgumentException(name + ": " + n + " (expected: >= " + expected + ')');
    }

    return n;
  }
}
