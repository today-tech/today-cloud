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
