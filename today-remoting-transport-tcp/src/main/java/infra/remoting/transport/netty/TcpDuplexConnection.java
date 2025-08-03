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

import infra.remoting.DuplexConnection;
import infra.remoting.RSocketErrorException;
import infra.remoting.frame.ErrorFrameCodec;
import infra.remoting.frame.FrameLengthCodec;
import infra.remoting.internal.BaseDuplexConnection;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;

/** An implementation of {@link DuplexConnection} that connects via TCP. */
public final class TcpDuplexConnection extends BaseDuplexConnection {
  private final String side;
  private final Connection connection;

  /**
   * Creates a new instance
   *
   * @param connection the {@link Connection} for managing the server
   */
  public TcpDuplexConnection(Connection connection) {
    this("unknown", connection);
  }

  /**
   * Creates a new instance
   *
   * @param connection the {@link Connection} for managing the server
   */
  public TcpDuplexConnection(String side, Connection connection) {
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
  public void sendErrorAndClose(RSocketErrorException e) {
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
    return "TcpDuplexConnection{side='%s', connection=%s}".formatted(side, connection);
  }

}
