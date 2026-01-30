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
