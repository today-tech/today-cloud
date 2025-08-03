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

package infra.remoting;

import org.reactivestreams.Subscriber;

import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Flux;

/**
 * Represents a connection with input/output that the protocol uses.
 */
public interface DuplexConnection extends Availability, Closeable {

  /**
   * Delivers the given frame to the underlying transport connection. This method is non-blocking
   * and can be safely executed from multiple threads. This method does not provide any flow-control
   * mechanism.
   *
   * @param streamId to which the given frame relates
   * @param frame with the encoded content
   */
  void sendFrame(int streamId, ByteBuf frame);

  /**
   * Send an error frame and after it is successfully sent, close the connection.
   *
   * @param errorException to encode in the error frame
   */
  void sendErrorAndClose(ProtocolErrorException errorException);

  /**
   * Returns a stream of all {@code Frame}s received on this connection.
   *
   * <p><strong>Completion</strong>
   *
   * <p>Returned {@code Publisher} <em>MUST</em> never emit a completion event ({@link
   * Subscriber#onComplete()}).
   *
   * <p><strong>Error</strong>
   *
   * <p>Returned {@code Publisher} can error with various transport errors. If the underlying
   * physical connection is closed by the peer, then the returned stream from here <em>MUST</em>
   * emit an {@link ClosedChannelException}.
   *
   * <p><strong>Multiple Subscriptions</strong>
   *
   * <p>Returned {@code Publisher} is not required to support multiple concurrent subscriptions.
   * Channel will never have multiple subscriptions to this source. Implementations <em>MUST</em>
   * emit an {@link IllegalStateException} for subsequent concurrent subscriptions, if they do not
   * support multiple concurrent subscriptions.
   *
   * @return Stream of all {@code Frame}s received.
   */
  Flux<ByteBuf> receive();

  /**
   * Returns the assigned {@link ByteBufAllocator}.
   *
   * @return the {@link ByteBufAllocator}
   */
  ByteBufAllocator alloc();

  /**
   * Return the remote address that this connection is connected to. The returned {@link
   * SocketAddress} varies by transport type and should be downcast to obtain more detailed
   * information. For TCP and WebSocket, the address type is {@link java.net.InetSocketAddress}.
   *
   * @return the address
   */
  SocketAddress remoteAddress();

  @Override
  default double availability() {
    return isDisposed() ? 0.0 : 1.0;
  }
}
