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

package infra.remoting.transport;

import infra.remoting.Connection;
import reactor.core.publisher.Mono;

/**
 * A contract to accept a new {@code Connection}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/2 22:06
 */
public interface ConnectionAcceptor {

  /**
   * Accept a new {@code Connection} and returns {@code Publisher} signifying the end of
   * processing of the connection.
   *
   * @param connection New {@code Connection} to be processed.
   * @return A {@code Publisher} which terminates when the processing of the connection finishes.
   */
  Mono<Void> accept(Connection connection);

}
