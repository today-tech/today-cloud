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

package infra.remoting.transport.local;

import java.util.Objects;

import infra.remoting.Connection;
import infra.remoting.internal.UnboundedProcessor;
import infra.remoting.transport.ClientTransport;
import infra.remoting.transport.ConnectionAcceptor;
import infra.remoting.transport.ServerTransport;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * An implementation of {@link ClientTransport} that connects to a {@link ServerTransport} in the
 * same JVM.
 */
public final class LocalClientTransport implements ClientTransport {

  private final String name;

  private final ByteBufAllocator allocator;

  private LocalClientTransport(String name, ByteBufAllocator allocator) {
    this.name = name;
    this.allocator = allocator;
  }

  /**
   * Creates a new instance.
   *
   * @param name the name of the {@link ClientTransport} instance to connect to
   * @return a new instance
   * @throws NullPointerException if {@code name} is {@code null}
   */
  public static LocalClientTransport create(String name) {
    Objects.requireNonNull(name, "name is required");

    return create(name, ByteBufAllocator.DEFAULT);
  }

  /**
   * Creates a new instance.
   *
   * @param name the name of the {@link ClientTransport} instance to connect to
   * @param allocator the allocator used by {@link ClientTransport} instance
   * @return a new instance
   * @throws NullPointerException if {@code name} is {@code null}
   */
  public static LocalClientTransport create(String name, ByteBufAllocator allocator) {
    Objects.requireNonNull(name, "name is required");
    Objects.requireNonNull(allocator, "allocator is required");

    return new LocalClientTransport(name, allocator);
  }

  @Override
  public Mono<Connection> connect() {
    return Mono.defer(() -> {
      ConnectionAcceptor server = LocalServerTransport.findServer(name);
      if (server == null) {
        return Mono.error(new IllegalArgumentException("Could not find server: " + name));
      }

      Sinks.One<Object> inSink = Sinks.one();
      Sinks.One<Object> outSink = Sinks.one();
      UnboundedProcessor in = new UnboundedProcessor(inSink::tryEmitEmpty);
      UnboundedProcessor out = new UnboundedProcessor(outSink::tryEmitEmpty);

      Mono<Void> onClose = inSink.asMono().and(outSink.asMono());

      server.accept(new LocalConnection(name, allocator, out, in, onClose)).subscribe();

      return Mono.<Connection>just(
              new LocalConnection(name, allocator, in, out, onClose));
    });
  }
}
