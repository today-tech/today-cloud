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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import infra.lang.Nullable;
import infra.remoting.Closeable;
import infra.remoting.DuplexConnection;
import infra.remoting.transport.ClientTransport;
import infra.remoting.transport.ConnectionAcceptor;
import infra.remoting.transport.ServerTransport;
import reactor.core.Scannable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * An implementation of {@link ServerTransport} that connects to a {@link ClientTransport} in the
 * same JVM.
 */
public final class LocalServerTransport implements ServerTransport<Closeable> {

  private static final ConcurrentMap<String, ServerCloseableAcceptor> registry =
          new ConcurrentHashMap<>();

  private final String name;

  private LocalServerTransport(String name) {
    this.name = name;
  }

  /**
   * Creates an instance.
   *
   * @param name the name of this {@link ServerTransport} that clients will connect to
   * @return a new instance
   * @throws NullPointerException if {@code name} is {@code null}
   */
  public static LocalServerTransport create(String name) {
    Objects.requireNonNull(name, "name is required");
    return new LocalServerTransport(name);
  }

  /**
   * Creates an instance with a random name.
   *
   * @return a new instance with a random name
   */
  public static LocalServerTransport createEphemeral() {
    return create(UUID.randomUUID().toString());
  }

  /**
   * Remove an instance from the JVM registry.
   *
   * @param name the local transport instance to free
   * @throws NullPointerException if {@code name} is {@code null}
   */
  public static void dispose(String name) {
    Objects.requireNonNull(name, "name is required");
    ServerCloseableAcceptor sca = registry.remove(name);
    if (sca != null) {
      sca.dispose();
    }
  }

  /**
   * Retrieves an instance of {@link ConnectionAcceptor} based on the name of its {@code
   * LocalServerTransport}. Returns {@code null} if that server is not registered.
   *
   * @param name the name of the server to retrieve
   * @return the server if it has been registered, {@code null} otherwise
   * @throws NullPointerException if {@code name} is {@code null}
   */
  @Nullable
  static ConnectionAcceptor findServer(String name) {
    Objects.requireNonNull(name, "name is required");

    return registry.get(name);
  }

  /** Return the name associated with this local server instance. */
  String getName() {
    return name;
  }

  /**
   * Return a new {@link LocalClientTransport} connected to this {@code LocalServerTransport}
   * through its {@link #getName()}.
   */
  public LocalClientTransport clientTransport() {
    return LocalClientTransport.create(name);
  }

  @Override
  public Mono<Closeable> start(ConnectionAcceptor acceptor) {
    Objects.requireNonNull(acceptor, "acceptor is required");
    return Mono.create(sink -> {
      ServerCloseableAcceptor closeable = new ServerCloseableAcceptor(name, acceptor);
      if (registry.putIfAbsent(name, closeable) != null) {
        sink.error(new IllegalStateException("name already registered: " + name));
      }
      sink.success(closeable);
    });
  }

  @SuppressWarnings({ "ReactorTransformationOnMonoVoid", "CallingSubscribeInNonBlockingScope" })
  static class ServerCloseableAcceptor implements ConnectionAcceptor, Closeable {

    private final LocalSocketAddress address;

    private final ConnectionAcceptor acceptor;

    private final Set<DuplexConnection> activeConnections = ConcurrentHashMap.newKeySet();

    private final Sinks.Empty<Void> onClose = Sinks.unsafe().empty();

    ServerCloseableAcceptor(String name, ConnectionAcceptor acceptor) {
      Objects.requireNonNull(name, "name is required");
      this.address = new LocalSocketAddress(name);
      this.acceptor = acceptor;
    }

    @Override
    public Mono<Void> accept(DuplexConnection duplexConnection) {
      activeConnections.add(duplexConnection);
      duplexConnection
              .onClose()
              .doFinally(__ -> activeConnections.remove(duplexConnection))
              .subscribe();
      return acceptor.accept(duplexConnection);
    }

    @Override
    public void dispose() {
      if (!registry.remove(address.getName(), this)) {
        // already disposed
        return;
      }

      Mono.whenDelayError(activeConnections.stream().peek(DuplexConnection::dispose)
                      .map(DuplexConnection::onClose).collect(Collectors.toList()))
              .subscribe(null, onClose::tryEmitError, onClose::tryEmitEmpty);
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public boolean isDisposed() {
      return onClose.scan(Scannable.Attr.TERMINATED) || onClose.scan(Scannable.Attr.CANCELLED);
    }

    @Override
    public Mono<Void> onClose() {
      return onClose.asMono();
    }
  }
}
