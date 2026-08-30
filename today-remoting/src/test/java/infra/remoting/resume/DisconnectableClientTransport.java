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

package infra.remoting.resume;

import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import infra.remoting.Connection;
import infra.remoting.transport.ClientTransport;
import reactor.core.publisher.Mono;

class DisconnectableClientTransport implements ClientTransport {

  private final ClientTransport clientTransport;

  private final AtomicReference<Connection> curConnection = new AtomicReference<>();

  private long nextConnectPermitMillis;

  public DisconnectableClientTransport(ClientTransport clientTransport) {
    this.clientTransport = clientTransport;
  }

  @Override
  public Mono<Connection> connect() {
    return Mono.defer(() ->
            now() < nextConnectPermitMillis
                    ? Mono.error(new ClosedChannelException())
                    : clientTransport.connect().map(c -> {
                      if (curConnection.compareAndSet(null, c)) {
                        return c;
                      }
                      else {
                        throw new IllegalStateException(
                                "Transport supports at most 1 connection");
                      }
                    }));
  }

  public void disconnect() {
    disconnectFor(Duration.ZERO);
  }

  public void disconnectPermanently() {
    disconnectFor(Duration.ofDays(42));
  }

  public void disconnectFor(Duration cooldown) {
    Connection cur = curConnection.getAndSet(null);
    if (cur != null) {
      nextConnectPermitMillis = now() + cooldown.toMillis();
      cur.dispose();
    }
    else {
      throw new IllegalStateException("Trying to disconnect while not connected");
    }
  }

  private static long now() {
    return System.currentTimeMillis();
  }
}
