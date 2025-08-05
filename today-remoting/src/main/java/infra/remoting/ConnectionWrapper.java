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

import java.net.SocketAddress;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/3 11:25
 */
public class ConnectionWrapper implements Connection {

  private final Connection delegate;

  public ConnectionWrapper(Connection delegate) {
    this.delegate = delegate;
  }

  @Override
  public ByteBufAllocator alloc() {
    return delegate.alloc();
  }

  @Override
  public double availability() {
    return delegate.availability();
  }

  @Override
  public Flux<ByteBuf> receive() {
    return delegate.receive();
  }

  @Override
  public SocketAddress remoteAddress() {
    return delegate.remoteAddress();
  }

  @Override
  public void sendErrorAndClose(ProtocolErrorException errorException) {
    delegate.sendErrorAndClose(errorException);
  }

  @Override
  public void sendFrame(int streamId, ByteBuf frame) {
    delegate.sendFrame(streamId, frame);
  }

  @Override
  public Mono<Void> onClose() {
    return delegate.onClose();
  }

  @Override
  public void dispose() {
    delegate.dispose();
  }

  @Override
  public boolean isDisposed() {
    return delegate.isDisposed();
  }

}
