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

import java.net.SocketAddress;
import java.util.Objects;

import infra.remoting.Connection;
import infra.remoting.ProtocolErrorException;
import infra.remoting.frame.ErrorFrameCodec;
import infra.remoting.internal.BaseConnection;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * An implementation of {@link Connection} that connects via a Websocket.
 *
 * <p>strongly assumes that each ByteBuf is encoded with the length. This is not true
 * for message oriented transports so this must be specifically dropped from Frames sent and
 * stitched back on for frames received.
 */
public final class WebsocketConnection extends BaseConnection {
  private final String side;
  private final reactor.netty.Connection connection;

  /**
   * Creates a new instance
   *
   * @param connection the {@link reactor.netty.Connection} to for managing the server
   */
  public WebsocketConnection(reactor.netty.Connection connection) {
    this("unknown", connection);
  }

  /**
   * Creates a new instance
   *
   * @param connection the {@link reactor.netty.Connection} to for managing the server
   */
  public WebsocketConnection(String side, reactor.netty.Connection connection) {
    this.connection = Objects.requireNonNull(connection, "connection is required");
    this.side = side;

    connection
            .outbound()
            .sendObject(sender.map(BinaryWebSocketFrame::new))
            .then()
            .doFinally(__ -> connection.dispose())
            .subscribe();
  }

  @Override
  public ByteBufAllocator alloc() {
    return connection.channel().alloc();
  }

  @Override
  public SocketAddress remoteAddress() {
    return connection.channel().remoteAddress();
  }

  @Override
  protected void doOnClose() {
    connection.dispose();
  }

  @Override
  public Mono<Void> onClose() {
    return Mono.whenDelayError(super.onClose(), connection.onTerminate());
  }

  @Override
  public Flux<ByteBuf> receive() {
    return connection.inbound().receive();
  }

  @Override
  public void sendErrorAndClose(ProtocolErrorException e) {
    final ByteBuf errorFrame = ErrorFrameCodec.encode(alloc(), 0, e);
    sender.tryEmitFinal(errorFrame);
  }

  @Override
  public String toString() {
    return "WebsocketConnection{"
            + "side='"
            + side
            + '\''
            + ", connection="
            + connection
            + '}';
  }
}
