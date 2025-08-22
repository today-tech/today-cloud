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

package infra.remoting.transport.netty;

import java.net.SocketAddress;
import java.util.Objects;

import infra.remoting.Connection;
import infra.remoting.ProtocolErrorException;
import infra.remoting.frame.ErrorFrameCodec;
import infra.remoting.frame.FrameLengthCodec;
import infra.remoting.internal.BaseConnection;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** An implementation of {@link Connection} that connects via TCP. */
public final class TcpConnection extends BaseConnection {

  private final String side;
  private final reactor.netty.Connection connection;

  /**
   * Creates a new instance
   *
   * @param connection the {@link reactor.netty.Connection} for managing the server
   */
  public TcpConnection(reactor.netty.Connection connection) {
    this("unknown", connection);
  }

  /**
   * Creates a new instance
   *
   * @param connection the {@link reactor.netty.Connection} for managing the server
   */
  public TcpConnection(String side, reactor.netty.Connection connection) {
    this.connection = Objects.requireNonNull(connection, "connection is required");
    this.side = side;

    connection.outbound().send(sender).then().doFinally(__ -> connection.dispose()).subscribe();
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
  public void sendErrorAndClose(ProtocolErrorException e) {
    final ByteBuf errorFrame = ErrorFrameCodec.encode(alloc(), 0, e);
    sender.tryEmitFinal(FrameLengthCodec.encode(alloc(), errorFrame.readableBytes(), errorFrame));
  }

  @Override
  public Flux<ByteBuf> receive() {
    return connection.inbound().receive().map(FrameLengthCodec::frame);
  }

  @Override
  public void sendFrame(int streamId, ByteBuf frame) {
    super.sendFrame(streamId, FrameLengthCodec.encode(alloc(), frame.readableBytes(), frame));
  }

  @Override
  public String toString() {
    return "TcpConnection{side='%s', connection=%s}".formatted(side, connection);
  }

}
