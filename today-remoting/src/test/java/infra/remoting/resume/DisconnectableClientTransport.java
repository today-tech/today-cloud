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
