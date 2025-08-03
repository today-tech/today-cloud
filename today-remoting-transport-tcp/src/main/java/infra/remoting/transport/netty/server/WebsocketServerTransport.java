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

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Objects;

import infra.remoting.transport.netty.WebsocketDuplexConnection;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.ConnectionAcceptor;
import io.rsocket.transport.ServerTransport;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.http.server.HttpServer;

/**
 * An implementation of {@link ServerTransport} that connects to a {@link ClientTransport} via a
 * Websocket.
 */
public final class WebsocketServerTransport extends BaseWebsocketServerTransport<WebsocketServerTransport, CloseableChannel> {

  private final HttpServer server;

  private HttpHeaders headers = new DefaultHttpHeaders();

  private WebsocketServerTransport(HttpServer server) {
    this.server = serverConfigurer.apply(Objects.requireNonNull(server, "server is required"));
  }

  /**
   * Creates a new instance binding to localhost
   *
   * @param port the port to bind to
   * @return a new instance
   */
  public static WebsocketServerTransport create(int port) {
    HttpServer httpServer = HttpServer.create().port(port);
    return create(httpServer);
  }

  /**
   * Creates a new instance
   *
   * @param bindAddress the address to bind to
   * @param port the port to bind to
   * @return a new instance
   * @throws NullPointerException if {@code bindAddress} is {@code null}
   */
  public static WebsocketServerTransport create(String bindAddress, int port) {
    Objects.requireNonNull(bindAddress, "bindAddress is required");
    HttpServer httpServer = HttpServer.create().host(bindAddress).port(port);
    return create(httpServer);
  }

  /**
   * Creates a new instance
   *
   * @param address the address to bind to
   * @return a new instance
   * @throws NullPointerException if {@code address} is {@code null}
   */
  public static WebsocketServerTransport create(InetSocketAddress address) {
    Objects.requireNonNull(address, "address is required");
    return create(address.getHostName(), address.getPort());
  }

  /**
   * Creates a new instance
   *
   * @param server the {@link HttpServer} to use
   * @return a new instance
   * @throws NullPointerException if {@code server} is {@code null}
   */
  public static WebsocketServerTransport create(final HttpServer server) {
    Objects.requireNonNull(server, "server is required");
    return new WebsocketServerTransport(server);
  }

  /**
   * Add a header and value(s) to set on the response of WebSocket handshakes.
   *
   * @param name the header name
   * @param values the header value(s)
   * @return the same instance for method chaining
   */
  public WebsocketServerTransport header(String name, String... values) {
    if (values != null) {
      Arrays.stream(values).forEach(value -> headers.add(name, value));
    }
    return this;
  }

  @Override
  public Mono<CloseableChannel> start(ConnectionAcceptor acceptor) {
    Objects.requireNonNull(acceptor, "acceptor is required");
    return server.handle((request, response) -> {
              response.headers(headers);
              return response.sendWebsocket(
                      (in, out) ->
                              acceptor
                                      .accept(new WebsocketDuplexConnection("server", (Connection) in))
                                      .then(out.neverComplete()),
                      specBuilder.build());
            })
            .bind()
            .map(CloseableChannel::new);
  }
}
