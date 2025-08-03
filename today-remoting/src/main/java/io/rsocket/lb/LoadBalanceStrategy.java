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

import java.util.List;

import io.rsocket.Channel;

/**
 * Strategy to select an {@link Channel} given a list of instances for load-balancing purposes. A
 * simple implementation might go in round-robin fashion while a more sophisticated strategy might
 * check availability, track usage stats, and so on.
 */
@FunctionalInterface
public interface LoadBalanceStrategy {

  /**
   * Select an {@link Channel} from the given non-empty list.
   *
   * @param channels the list to choose from
   * @return the selected instance
   */
  Channel select(List<Channel> channels);

}
