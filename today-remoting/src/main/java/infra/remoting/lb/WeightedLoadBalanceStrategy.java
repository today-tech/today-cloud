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

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import infra.lang.Nullable;
import infra.remoting.Channel;
import infra.remoting.core.ChannelConnector;

/**
 * {@link LoadBalanceStrategy} that assigns a weight to each {@code RSocket} based on {@link
 * Channel#availability() availability} and usage statistics. The weight is used to decide which
 * {@code RSocket} to select.
 *
 * <p>Use {@link #create()} or a {@link #builder() Builder} to create an instance.
 *
 * @see <a href="https://www.youtube.com/watch?v=6NdxUY1La2I">Predictive Load-Balancing: Unfair but
 * Faster & more Robust</a>
 * @see WeightedStatsRequestInterceptor
 */
public class WeightedLoadBalanceStrategy implements ClientLoadBalanceStrategy {

  private static final double EXP_FACTOR = 4.0;

  final int maxPairSelectionAttempts;

  final Function<Channel, WeightedStats> weightedStatsResolver;

  private WeightedLoadBalanceStrategy(int numberOfAttempts, @Nullable Function<Channel, WeightedStats> resolver) {
    this.maxPairSelectionAttempts = numberOfAttempts;
    this.weightedStatsResolver = (resolver != null ? resolver : new DefaultWeightedStatsResolver());
  }

  @Override
  public void initialize(ChannelConnector connector) {
    final Function<Channel, WeightedStats> resolver = weightedStatsResolver;
    if (resolver instanceof DefaultWeightedStatsResolver) {
      ((DefaultWeightedStatsResolver) resolver).init(connector);
    }
  }

  @Override
  public Channel select(List<Channel> channels) {
    final int size = channels.size();

    Channel weightedChannel;
    final var weightedStatsResolver = this.weightedStatsResolver;
    switch (size) {
      case 1:
        weightedChannel = channels.get(0);
        break;
      case 2: {
        Channel rsc1 = channels.get(0);
        Channel rsc2 = channels.get(1);

        double w1 = algorithmicWeight(rsc1, weightedStatsResolver.apply(rsc1));
        double w2 = algorithmicWeight(rsc2, weightedStatsResolver.apply(rsc2));
        if (w1 < w2) {
          weightedChannel = rsc2;
        }
        else {
          weightedChannel = rsc1;
        }
      }
      break;
      default: {
        Channel rsc1 = null;
        Channel rsc2 = null;

        ThreadLocalRandom localRandom = ThreadLocalRandom.current();
        for (int i = 0; i < this.maxPairSelectionAttempts; i++) {
          int i1 = localRandom.nextInt(size);
          int i2 = localRandom.nextInt(size - 1);

          if (i2 >= i1) {
            i2++;
          }
          rsc1 = channels.get(i1);
          rsc2 = channels.get(i2);
          if (rsc1.availability() > 0.0 && rsc2.availability() > 0.0) {
            break;
          }
        }

        if (rsc1 != null & rsc2 != null) {
          double w1 = algorithmicWeight(rsc1, weightedStatsResolver.apply(rsc1));
          double w2 = algorithmicWeight(rsc2, weightedStatsResolver.apply(rsc2));

          if (w1 < w2) {
            weightedChannel = rsc2;
          }
          else {
            weightedChannel = rsc1;
          }
        }
        else if (rsc1 != null) {
          weightedChannel = rsc1;
        }
        else {
          weightedChannel = rsc2;
        }
      }
    }

    return weightedChannel;
  }

  private static double algorithmicWeight(Channel channel, @Nullable final WeightedStats weightedStats) {
    if (weightedStats == null) {
      return 1.0;
    }
    if (channel.isDisposed() || channel.availability() == 0.0) {
      return 0.0;
    }
    final int pending = weightedStats.pending();

    double latency = weightedStats.predictedLatency();

    final double low = weightedStats.lowerQuantileLatency();
    final double high = Math.max(
            weightedStats.higherQuantileLatency(),
            low * 1.001); // ensure higherQuantile > lowerQuantile + .1%
    final double bandWidth = Math.max(high - low, 1);

    if (latency < low) {
      latency /= calculateFactor(low, latency, bandWidth);
    }
    else if (latency > high) {
      latency *= calculateFactor(latency, high, bandWidth);
    }

    return (channel.availability() * weightedStats.weightedAvailability())
            / (1.0d + latency * (pending + 1));
  }

  private static double calculateFactor(final double u, final double l, final double bandWidth) {
    final double alpha = (u - l) / bandWidth;
    return Math.pow(1 + alpha, EXP_FACTOR);
  }

  /**
   * Create an instance of {@link WeightedLoadBalanceStrategy} with default settings, which include
   * round-robin load-balancing and 5 {@link #maxPairSelectionAttempts}.
   */
  public static WeightedLoadBalanceStrategy create() {
    return new Builder().build();
  }

  /** Return a builder to create a {@link WeightedLoadBalanceStrategy} with. */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for {@link WeightedLoadBalanceStrategy}.
   */
  public static class Builder {

    private int maxPairSelectionAttempts = 5;

    @Nullable
    private Function<Channel, WeightedStats> weightedStatsResolver;

    private Builder() {
    }

    /**
     * How many times to try to randomly select a pair of RSocket connections with non-zero
     * availability. This is applicable when there are more than two connections in the pool. If the
     * number of attempts is exceeded, the last selected pair is used.
     *
     * <p>By default this is set to 5.
     *
     * @param numberOfAttempts the iteration count
     */
    public Builder maxPairSelectionAttempts(int numberOfAttempts) {
      this.maxPairSelectionAttempts = numberOfAttempts;
      return this;
    }

    /**
     * Configure how the created {@link WeightedLoadBalanceStrategy} should find the stats for a
     * given RSocket.
     *
     * <p>By default this resolver is not set.
     *
     * <p>When {@code WeightedLoadbalanceStrategy} is used through the {@link
     * LoadBalanceRemotingClient}, the resolver does not need to be set because a {@link
     * WeightedStatsRequestInterceptor} is automatically installed through the {@link
     * ClientLoadBalanceStrategy} callback. If this strategy is used in any other context however, a
     * resolver here must be provided.
     *
     * @param resolver to find the stats for an RSocket with
     */
    public Builder weightedStatsResolver(Function<Channel, WeightedStats> resolver) {
      this.weightedStatsResolver = resolver;
      return this;
    }

    /** Build the {@code WeightedLoadbalanceStrategy} instance. */
    public WeightedLoadBalanceStrategy build() {
      return new WeightedLoadBalanceStrategy(
              this.maxPairSelectionAttempts, this.weightedStatsResolver);
    }
  }

  private static class DefaultWeightedStatsResolver implements Function<Channel, WeightedStats> {

    final ConcurrentHashMap<Channel, WeightedStats> statsMap = new ConcurrentHashMap<>();

    @Override
    public WeightedStats apply(Channel channel) {
      return statsMap.get(channel);
    }

    void init(ChannelConnector connector) {
      connector.interceptors(registry -> registry.forRequestsInRequester(rSocket -> {
        final WeightedStatsRequestInterceptor interceptor = new WeightedStatsRequestInterceptor() {
          @Override
          public void dispose() {
            statsMap.remove(rSocket);
          }
        };
        statsMap.put(rSocket, interceptor);
        return interceptor;
      }));
    }
  }
}
