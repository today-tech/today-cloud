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

import org.reactivestreams.Publisher;

import io.rsocket.core.ChannelConnector;
import io.rsocket.transport.ClientTransport;

/**
 * Representation for a load-balance target used as input to {@link LoadBalanceRemotingClient} that
 * in turn maintains and periodically updates a list of current load-balance targets. The {@link
 * #getKey()} is used to identify a target uniquely while the {@link #getTransport() transport} is
 * used to connect to the target server.
 *
 * @see LoadBalanceRemotingClient#create(ChannelConnector, Publisher)
 */
public class LoadBalanceTarget {

  private final String key;

  private final ClientTransport transport;

  private LoadBalanceTarget(String key, ClientTransport transport) {
    this.key = key;
    this.transport = transport;
  }

  /** Return the key that identifies this target uniquely. */
  public String getKey() {
    return key;
  }

  /** Return the transport to use to connect to the target server. */
  public ClientTransport getTransport() {
    return transport;
  }

  /**
   * Create a new {@link LoadBalanceTarget} with the given key and {@link ClientTransport}. The key
   * can be anything that identifies the target uniquely, e.g. SocketAddress, URL, and so on.
   *
   * @param key identifies the load-balance target uniquely
   * @param transport for connecting to the target
   * @return the created instance
   */
  public static LoadBalanceTarget of(String key, ClientTransport transport) {
    return new LoadBalanceTarget(key, transport);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    LoadBalanceTarget that = (LoadBalanceTarget) other;
    return key.equals(that.key);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

}
