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

package infra.cloud.provider;

import infra.remoting.Closeable;
import infra.remoting.transport.ServerTransport;

/**
 * Factory interface for creating {@link ServerTransport} instances.
 * <p>
 * Implementations of this interface are responsible for instantiating and configuring
 * server transport objects that handle incoming network connections and communication.
 *
 * @param <T> the type of closeable resource associated with the transport
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see ServerTransport
 * @since 1.0 2025/8/22 22:18
 */
public interface ServerTransportFactory<T extends Closeable> {

  /**
   * Creates a new {@link ServerTransport} instance.
   * <p>
   * The returned transport is ready to accept connections and handle communication
   * according to its specific implementation details.
   *
   * @return a new {@code ServerTransport} instance
   */
  ServerTransport<T> createTransport();

}
