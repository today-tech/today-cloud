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

import io.rsocket.Channel;

/**
 * Contract to expose the stats required in {@link WeightedLoadBalanceStrategy} to calculate an
 * algorithmic weight for an {@code Channel}. The weight helps to select an {@code Channel} for
 * load-balancing.
 */
public interface WeightedStats {

  double higherQuantileLatency();

  double lowerQuantileLatency();

  int pending();

  double predictedLatency();

  double weightedAvailability();

  /**
   * Create a proxy for the given {@code Channel} that attaches the stats contained in this instance
   * and exposes them as {@link WeightedStats}.
   *
   * @param channel the Channel to wrap
   * @return the wrapped Channel
   */
  default Channel wrap(Channel channel) {
    return new WeightedStatsChannel(channel, this);
  }

}
