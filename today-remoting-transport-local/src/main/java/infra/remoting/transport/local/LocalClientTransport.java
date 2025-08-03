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

package infra.remoting.transport.local;

import java.util.Objects;

import io.netty.buffer.ByteBufAllocator;
import io.rsocket.DuplexConnection;
import io.rsocket.internal.UnboundedProcessor;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.ConnectionAcceptor;
import io.rsocket.transport.ServerTransport;
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
  public Mono<DuplexConnection> connect() {
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

      server.accept(new LocalDuplexConnection(name, allocator, out, in, onClose)).subscribe();

      return Mono.<DuplexConnection>just(
              new LocalDuplexConnection(name, allocator, in, out, onClose));
    });
  }
}
