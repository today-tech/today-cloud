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

import infra.remoting.Channel;
import infra.remoting.util.ChannelWrapper;

/**
 * Package private {@code ChannelWrapper} used from {@link WeightedStats#wrap(Channel)} to attach a
 * {@link WeightedStats} instance to an {@code Channel}.
 */
final class WeightedStatsChannel extends ChannelWrapper implements WeightedStats {

  private final WeightedStats weightedStats;

  public WeightedStatsChannel(Channel delegate, WeightedStats weightedStats) {
    super(delegate);
    this.weightedStats = weightedStats;
  }

  @Override
  public double higherQuantileLatency() {
    return this.weightedStats.higherQuantileLatency();
  }

  @Override
  public double lowerQuantileLatency() {
    return this.weightedStats.lowerQuantileLatency();
  }

  @Override
  public int pending() {
    return this.weightedStats.pending();
  }

  @Override
  public double predictedLatency() {
    return this.weightedStats.predictedLatency();
  }

  @Override
  public double weightedAvailability() {
    return this.weightedStats.weightedAvailability();
  }

}
