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

package infra.remoting.transport.netty.server;

import org.reactivestreams.Publisher;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import infra.remoting.Closeable;
import infra.remoting.transport.ConnectionAcceptor;
import infra.remoting.transport.ServerTransport;
import infra.remoting.transport.netty.WebsocketDuplexConnection;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRoutes;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;

/**
 * An implementation of {@link ServerTransport} that connects via Websocket and listens on specified
 * routes.
 */
public final class WebsocketRouteTransport
        extends BaseWebsocketServerTransport<WebsocketRouteTransport, Closeable> {

  private final String path;

  private final Consumer<? super HttpServerRoutes> routesBuilder;

  private final HttpServer server;

  /**
   * Creates a new instance
   *
   * @param server the {@link HttpServer} to use
   * @param routesBuilder the builder for the routes that will be listened on
   * @param path the path foe each route
   */
  public WebsocketRouteTransport(
          HttpServer server, Consumer<? super HttpServerRoutes> routesBuilder, String path) {
    this.server = serverConfigurer.apply(Objects.requireNonNull(server, "server is required"));
    this.routesBuilder = Objects.requireNonNull(routesBuilder, "routesBuilder is required");
    this.path = Objects.requireNonNull(path, "path is required");
  }

  @Override
  public Mono<Closeable> start(ConnectionAcceptor acceptor) {
    Objects.requireNonNull(acceptor, "acceptor is required");
    return server
            .route(
                    routes -> {
                      routesBuilder.accept(routes);
                      routes.ws(path, newHandler(acceptor), specBuilder.build());
                    })
            .bind()
            .map(CloseableChannel::new);
  }

  /**
   * Creates a new Websocket handler
   *
   * @param acceptor the {@link ConnectionAcceptor} to use with the handler
   * @return a new Websocket handler
   * @throws NullPointerException if {@code acceptor} is {@code null}
   */
  public static BiFunction<WebsocketInbound, WebsocketOutbound, Publisher<Void>> newHandler(
          ConnectionAcceptor acceptor) {
    return (in, out) ->
            acceptor
                    .accept(new WebsocketDuplexConnection("server", (Connection) in))
                    .then(out.neverComplete());
  }
}
