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

package infra.remoting.transport;

import infra.remoting.Closeable;
import reactor.core.publisher.Mono;

/**
 * A server contract for writing transports of RSocket.
 */
public interface ServerTransport<T extends Closeable> extends Transport {

  /**
   * Start this server.
   *
   * @param acceptor to process a newly accepted connections with
   * @return A handle for information about and control over the server.
   */
  Mono<T> start(ConnectionAcceptor acceptor);

}
