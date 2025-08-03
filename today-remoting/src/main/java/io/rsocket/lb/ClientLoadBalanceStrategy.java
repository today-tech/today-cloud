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

import io.rsocket.core.ChannelConnector;
import io.rsocket.plugins.InterceptorRegistry;

/**
 * A {@link LoadBalanceStrategy} with an interest in configuring the {@link ChannelConnector} for
 * connecting to load-balance targets in order to hook into request lifecycle and track usage
 * statistics.
 *
 * <p>Currently this callback interface is supported for strategies configured in {@link
 * LoadBalanceRemotingClient}.
 */
public interface ClientLoadBalanceStrategy extends LoadBalanceStrategy {

  /**
   * Initialize the connector, for example using the {@link InterceptorRegistry}, to intercept
   * requests.
   *
   * @param connector the connector to configure
   */
  void initialize(ChannelConnector connector);

}
