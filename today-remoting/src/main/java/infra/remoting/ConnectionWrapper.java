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

package infra.remoting;

import java.net.SocketAddress;
import java.util.Objects;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/3 11:25
 */
public class ConnectionWrapper implements Connection {

  protected final Connection delegate;

  public ConnectionWrapper(Connection delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate is required");
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
