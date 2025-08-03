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

package infra.remoting.transport.netty.client;

import java.net.InetSocketAddress;
import java.util.Objects;

import io.rsocket.DuplexConnection;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.ServerTransport;
import infra.remoting.transport.netty.RSocketLengthCodec;
import infra.remoting.transport.netty.TcpDuplexConnection;
import reactor.core.publisher.Mono;
import reactor.netty.tcp.TcpClient;

import static io.rsocket.frame.FrameLengthCodec.FRAME_LENGTH_MASK;

/**
 * An implementation of {@link ClientTransport} that connects to a {@link ServerTransport} via TCP.
 */
public final class TcpClientTransport implements ClientTransport {

  private final TcpClient client;
  private final int maxFrameLength;

  private TcpClientTransport(TcpClient client, int maxFrameLength) {
    this.client = client;
    this.maxFrameLength = maxFrameLength;
  }

  /**
   * Creates a new instance connecting to localhost
   *
   * @param port the port to connect to
   * @return a new instance
   */
  public static TcpClientTransport create(int port) {
    TcpClient tcpClient = TcpClient.create().port(port);
    return create(tcpClient);
  }

  /**
   * Creates a new instance
   *
   * @param bindAddress the address to connect to
   * @param port the port to connect to
   * @return a new instance
   * @throws NullPointerException if {@code bindAddress} is {@code null}
   */
  public static TcpClientTransport create(String bindAddress, int port) {
    Objects.requireNonNull(bindAddress, "bindAddress is required");

    TcpClient tcpClient = TcpClient.create().host(bindAddress).port(port);
    return create(tcpClient);
  }

  /**
   * Creates a new instance
   *
   * @param address the address to connect to
   * @return a new instance
   * @throws NullPointerException if {@code address} is {@code null}
   */
  public static TcpClientTransport create(InetSocketAddress address) {
    Objects.requireNonNull(address, "address is required");

    TcpClient tcpClient = TcpClient.create().remoteAddress(() -> address);
    return create(tcpClient);
  }

  /**
   * Creates a new instance
   *
   * @param client the {@link TcpClient} to use
   * @return a new instance
   * @throws NullPointerException if {@code client} is {@code null}
   */
  public static TcpClientTransport create(TcpClient client) {
    return create(client, FRAME_LENGTH_MASK);
  }

  /**
   * Creates a new instance
   *
   * @param client the {@link TcpClient} to use
   * @param maxFrameLength max frame length being sent over the connection
   * @return a new instance
   * @throws NullPointerException if {@code client} is {@code null}
   */
  public static TcpClientTransport create(TcpClient client, int maxFrameLength) {
    Objects.requireNonNull(client, "client is required");

    return new TcpClientTransport(client, maxFrameLength);
  }

  @Override
  public int getMaxFrameLength() {
    return maxFrameLength;
  }

  @Override
  public Mono<DuplexConnection> connect() {
    return client
            .doOnConnected(c -> c.addHandlerLast(new RSocketLengthCodec(maxFrameLength)))
            .connect()
            .map(connection -> new TcpDuplexConnection("client", connection));
  }
}
