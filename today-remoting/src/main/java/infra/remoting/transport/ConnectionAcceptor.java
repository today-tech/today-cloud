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

import infra.remoting.DuplexConnection;
import reactor.core.publisher.Mono;

/**
 * A contract to accept a new {@code DuplexConnection}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/2 22:06
 */
public interface ConnectionAcceptor {

  /**
   * Accept a new {@code DuplexConnection} and returns {@code Publisher} signifying the end of
   * processing of the connection.
   *
   * @param connection New {@code DuplexConnection} to be processed.
   * @return A {@code Publisher} which terminates when the processing of the connection finishes.
   */
  Mono<Void> accept(DuplexConnection connection);

}
