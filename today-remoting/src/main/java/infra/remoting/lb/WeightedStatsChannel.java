/*
 * Copyright 2021 - 2026 the TODAY authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.remoting.lb;

import infra.remoting.Channel;
import infra.remoting.DecoratingChannel;

/**
 * Package private {@code ChannelWrapper} used from {@link WeightedStats#wrap(Channel)} to attach a
 * {@link WeightedStats} instance to an {@code Channel}.
 */
final class WeightedStatsChannel extends DecoratingChannel implements WeightedStats {

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
