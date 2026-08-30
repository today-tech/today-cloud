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

package infra.remoting.transport.netty.client;

import java.net.InetSocketAddress;
import java.util.Objects;

import infra.remoting.Connection;
import infra.remoting.transport.ClientTransport;
import infra.remoting.transport.ServerTransport;
import infra.remoting.transport.netty.ProtocolFrameLengthCodec;
import infra.remoting.transport.netty.TcpConnection;
import reactor.core.publisher.Mono;
import reactor.netty.tcp.TcpClient;

import static infra.remoting.frame.FrameLengthCodec.FRAME_LENGTH_MASK;

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
  public Mono<Connection> connect() {
    return client
            .doOnConnected(c -> c.addHandlerLast(new ProtocolFrameLengthCodec(maxFrameLength)))
            .connect()
            .map(connection -> new TcpConnection("client", connection));
  }
}
