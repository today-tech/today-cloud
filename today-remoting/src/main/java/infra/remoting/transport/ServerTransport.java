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

import infra.remoting.Closeable;
import reactor.core.publisher.Mono;

/**
 * A server contract for writing transports of protocol.
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
