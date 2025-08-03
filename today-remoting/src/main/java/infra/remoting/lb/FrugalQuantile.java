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

package infra.remoting.lb;

import java.util.SplittableRandom;

/**
 * Reference: Ma, Qiang, S. Muthukrishnan, and Mark Sandler. "Frugal Streaming for Estimating
 * Quantiles." Space-Efficient Data Structures, Streams, and Algorithms. Springer Berlin Heidelberg,
 * 2013. 77-96.
 *
 * <p>More info: http://blog.aggregateknowledge.com/2013/09/16/sketch-of-the-day-frugal-streaming/
 */
class FrugalQuantile implements Quantile {

  final double increment;

  final SplittableRandom rnd;

  int step;
  int sign;
  double quantile;

  volatile double estimate;

  public FrugalQuantile(double quantile, double increment) {
    this.increment = increment;
    this.quantile = quantile;
    this.estimate = 0.0;
    this.step = 1;
    this.sign = 0;
    this.rnd = new SplittableRandom(System.nanoTime());
  }

  public FrugalQuantile(double quantile) {
    this(quantile, 1.0);
  }

  public synchronized void reset(double quantile) {
    this.quantile = quantile;
    this.estimate = 0.0;
    this.step = 1;
    this.sign = 0;
  }

  public double estimation() {
    return estimate;
  }

  @Override
  public synchronized void insert(double x) {
    if (sign == 0) {
      estimate = x;
      sign = 1;
    }
    else {
      final double v = rnd.nextDouble();
      final double estimate = this.estimate;

      if (x > estimate && v > (1 - quantile)) {
        higher(x);
      }
      else if (x < estimate && v > quantile) {
        lower(x);
      }
    }
  }

  private void higher(double x) {
    double estimate = this.estimate;

    step += (int) (sign * increment);

    if (step > 0) {
      estimate += step;
    }
    else {
      estimate += 1;
    }

    if (estimate > x) {
      step += (int) (x - estimate);
      estimate = x;
    }

    if (sign < 0) {
      step = 1;
    }

    sign = 1;

    this.estimate = estimate;
  }

  private void lower(double x) {
    double estimate = this.estimate;

    step -= (int) (sign * increment);

    if (step > 0) {
      estimate -= step;
    }
    else {
      estimate--;
    }

    if (estimate < x) {
      step += (int) (estimate - x);
      estimate = x;
    }

    if (sign > 0) {
      step = 1;
    }

    sign = -1;

    this.estimate = estimate;
  }

  @Override
  public String toString() {
    return "FrugalQuantile(q=%s, v=%s)".formatted(quantile, estimate);
  }

}
