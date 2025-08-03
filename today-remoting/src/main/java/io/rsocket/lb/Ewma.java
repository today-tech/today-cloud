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

package io.rsocket.lb;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import io.rsocket.util.Clock;

/**
 * Compute the exponential weighted moving average of a series of values. The time at which you
 * insert the value into `Ewma` is used to compute a weight (recent points are weighted higher). The
 * parameter for defining the convergence speed (like most decay process) is the half-life.
 *
 * <p>e.g. with a half-life of 10 unit, if you insert 100 at t=0 and 200 at t=10 the ewma will be
 * equal to (200 - 100)/2 = 150 (half of the distance between the new and the old value)
 */
class Ewma {

  final long tau;

  volatile long stamp;
  static final AtomicLongFieldUpdater<Ewma> STAMP =
          AtomicLongFieldUpdater.newUpdater(Ewma.class, "stamp");

  volatile double ewma;

  public Ewma(long halfLife, TimeUnit unit, double initialValue) {
    this.tau = Clock.unit().convert((long) (halfLife / Math.log(2)), unit);
    this.ewma = initialValue;
    STAMP.lazySet(this, 0L);
  }

  public synchronized void insert(double x) {
    final long now = Clock.now();
    final double elapsed = Math.max(0, now - stamp);

    STAMP.lazySet(this, now);

    double w = Math.exp(-elapsed / tau);
    ewma = w * ewma + (1.0 - w) * x;
  }

  public synchronized void reset(double value) {
    stamp = 0L;
    ewma = value;
  }

  public double value() {
    return ewma;
  }

  @Override
  public String toString() {
    return "Ewma(value=%s, age=%d)".formatted(ewma, Clock.now() - stamp);
  }

}
