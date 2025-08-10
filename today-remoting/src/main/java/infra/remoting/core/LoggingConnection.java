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

package infra.remoting.core;

import java.net.SocketAddress;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.remoting.Connection;
import infra.remoting.ProtocolErrorException;
import infra.remoting.frame.FrameUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

final class LoggingConnection implements Connection {

  private static final Logger LOGGER = LoggerFactory.getLogger("infra.remoting.FrameLogger");

  private final Connection source;

  LoggingConnection(Connection source) {
    this.source = source;
  }

  @Override
  public void dispose() {
    source.dispose();
  }

  @Override
  public Mono<Void> onClose() {
    return source.onClose();
  }

  @Override
  public void sendFrame(int streamId, ByteBuf frame) {
    LOGGER.debug("sending -> {}", FrameUtil.toString(frame));

    source.sendFrame(streamId, frame);
  }

  @Override
  public void sendErrorAndClose(ProtocolErrorException e) {
    LOGGER.debug("sending -> {}: {}", e.getClass().getSimpleName(), e.getMessage());

    source.sendErrorAndClose(e);
  }

  @Override
  public Flux<ByteBuf> receive() {
    return source.receive().doOnNext(frame -> LOGGER.debug("receiving -> {}", FrameUtil.toString(frame)));
  }

  @Override
  public ByteBufAllocator alloc() {
    return source.alloc();
  }

  @Override
  public SocketAddress remoteAddress() {
    return source.remoteAddress();
  }

  static Connection wrapIfEnabled(Connection source) {
    if (LOGGER.isDebugEnabled()) {
      return new LoggingConnection(source);
    }
    return source;
  }
}
