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

import org.reactivestreams.Publisher;

import infra.remoting.core.ChannelConnector;
import infra.remoting.core.RemotingClient;
import infra.remoting.transport.ClientTransport;

/**
 * Representation for a load-balance target used as input to {@link LoadBalanceRemotingClient} that
 * in turn maintains and periodically updates a list of current load-balance targets. The {@link
 * #getKey()} is used to identify a target uniquely while the {@link #getTransport() transport} is
 * used to connect to the target server.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see RemotingClient#forLoadBalance(ChannelConnector, Publisher)
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
