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

package infra.remoting.transport.websocket;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Objects;

import infra.remoting.transport.ClientTransport;
import infra.remoting.transport.ConnectionAcceptor;
import infra.remoting.transport.ServerTransport;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.http.server.HttpServer;

/**
 * An implementation of {@link ServerTransport} that connects to a {@link ClientTransport} via a
 * Websocket.
 */
public final class WebsocketServerTransport extends BaseWebsocketServerTransport<WebsocketServerTransport, CloseableChannel> {

  private final HttpServer server;

  private final HttpHeaders headers = new DefaultHttpHeaders();

  private WebsocketServerTransport(HttpServer server) {
    this.server = BaseWebsocketServerTransport.serverConfigurer.apply(Objects.requireNonNull(server, "server is required"));
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
                                      .accept(new WebsocketConnection("server", (Connection) in))
                                      .then(out.neverComplete()),
                      specBuilder.build());
            })
            .bind()
            .map(CloseableChannel::new);
  }
}
