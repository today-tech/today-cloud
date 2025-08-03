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

package io.rsocket.core;

import io.rsocket.lease.LeaseSender;
import reactor.core.publisher.Flux;

public final class LeaseSpec {

  LeaseSender sender = Flux::never;

  int maxPendingRequests = 256;

  LeaseSpec() {
  }

  public LeaseSpec sender(LeaseSender sender) {
    this.sender = sender;
    return this;
  }

  /**
   * Setup the maximum queued requests waiting for lease to be available. The default value is 256
   *
   * @param maxPendingRequests if set to 0 the requester will terminate the request immediately if
   * no leases is available
   */
  public LeaseSpec maxPendingRequests(int maxPendingRequests) {
    this.maxPendingRequests = maxPendingRequests;
    return this;
  }

}
