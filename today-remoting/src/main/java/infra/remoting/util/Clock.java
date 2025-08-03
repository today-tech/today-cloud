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

package infra.remoting.util;

import java.util.concurrent.TimeUnit;

/** Abstraction to get current time and durations. */
public final class Clock {

  private Clock() {
    // No Instances.
  }

  public static long now() {
    return System.nanoTime() / 1000;
  }

  public static long elapsedSince(long timestamp) {
    long t = now();
    return Math.max(0L, t - timestamp);
  }

  public static TimeUnit unit() {
    return TimeUnit.MICROSECONDS;
  }
}
