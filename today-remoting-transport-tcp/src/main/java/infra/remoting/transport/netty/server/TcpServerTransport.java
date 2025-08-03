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
import java.util.Objects;

import infra.remoting.transport.ClientTransport;
import infra.remoting.transport.ConnectionAcceptor;
import infra.remoting.transport.ServerTransport;
import infra.remoting.transport.netty.ProtocolFrameLengthCodec;
import infra.remoting.transport.netty.TcpDuplexConnection;
import reactor.core.publisher.Mono;
import reactor.netty.tcp.TcpServer;

import static infra.remoting.frame.FrameLengthCodec.FRAME_LENGTH_MASK;

/**
 * An implementation of {@link ServerTransport} that connects to a {@link ClientTransport} via TCP.
 */
public final class TcpServerTransport implements ServerTransport<CloseableChannel> {

  private final TcpServer server;

  private final int maxFrameLength;

  private TcpServerTransport(TcpServer server, int maxFrameLength) {
    this.server = server;
    this.maxFrameLength = maxFrameLength;
  }

  /**
   * Creates a new instance binding to localhost
   *
   * @param port the port to bind to
   * @return a new instance
   */
  public static TcpServerTransport create(int port) {
    TcpServer server = TcpServer.create().port(port);
    return create(server);
  }

  /**
   * Creates a new instance
   *
   * @param bindAddress the address to bind to
   * @param port the port to bind to
   * @return a new instance
   * @throws NullPointerException if {@code bindAddress} is {@code null}
   */
  public static TcpServerTransport create(String bindAddress, int port) {
    Objects.requireNonNull(bindAddress, "bindAddress is required");
    TcpServer server = TcpServer.create().host(bindAddress).port(port);
    return create(server);
  }

  /**
   * Creates a new instance
   *
   * @param address the address to bind to
   * @return a new instance
   * @throws NullPointerException if {@code address} is {@code null}
   */
  public static TcpServerTransport create(InetSocketAddress address) {
    Objects.requireNonNull(address, "address is required");
    return create(address.getHostName(), address.getPort());
  }

  /**
   * Creates a new instance
   *
   * @param server the {@link TcpServer} to use
   * @return a new instance
   * @throws NullPointerException if {@code server} is {@code null}
   */
  public static TcpServerTransport create(TcpServer server) {
    return create(server, FRAME_LENGTH_MASK);
  }

  /**
   * Creates a new instance
   *
   * @param server the {@link TcpServer} to use
   * @param maxFrameLength max frame length being sent over the connection
   * @return a new instance
   * @throws NullPointerException if {@code server} is {@code null}
   */
  public static TcpServerTransport create(TcpServer server, int maxFrameLength) {
    Objects.requireNonNull(server, "server is required");
    return new TcpServerTransport(server, maxFrameLength);
  }

  @Override
  public int getMaxFrameLength() {
    return maxFrameLength;
  }

  @Override
  public Mono<CloseableChannel> start(ConnectionAcceptor acceptor) {
    Objects.requireNonNull(acceptor, "acceptor is required");
    return server.doOnConnection(c -> {
              c.addHandlerLast(new ProtocolFrameLengthCodec(maxFrameLength));
              acceptor.accept(new TcpDuplexConnection("server", c))
                      .then(Mono.<Void>never())
                      .subscribe(c.disposeSubscriber());
            })
            .bind()
            .map(CloseableChannel::new);
  }
}
