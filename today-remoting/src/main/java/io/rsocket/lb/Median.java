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

/**
 * This implementation gives better results because it considers more data-point.
 */
class Median extends FrugalQuantile {

  public Median() {
    super(0.5, 1.0);
  }

  public synchronized void reset() {
    super.reset(0.5);
  }

  @Override
  public synchronized void insert(double x) {
    if (sign == 0) {
      estimate = x;
      sign = 1;
    }
    else {
      final double estimate = this.estimate;
      if (x > estimate) {
        greaterThanZero(x);
      }
      else if (x < estimate) {
        lessThanZero(x);
      }
    }
  }

  private void greaterThanZero(double x) {
    double estimate = this.estimate;

    step += sign;

    if (step > 0) {
      estimate += step;
    }
    else {
      estimate += 1;
    }

    if (estimate > x) {
      step += (x - estimate);
      estimate = x;
    }

    if (sign < 0) {
      step = 1;
    }

    sign = 1;

    this.estimate = estimate;
  }

  private void lessThanZero(double x) {
    double estimate = this.estimate;

    step -= sign;

    if (step > 0) {
      estimate -= step;
    }
    else {
      estimate--;
    }

    if (estimate < x) {
      step += (estimate - x);
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
    return "Median(v=%s)".formatted(estimate);
  }

}
