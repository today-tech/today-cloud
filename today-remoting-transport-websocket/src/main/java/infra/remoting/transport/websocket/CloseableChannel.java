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

import java.net.InetSocketAddress;
import java.util.Objects;

import infra.core.FutureMono;
import infra.remoting.Closeable;
import io.netty.channel.Channel;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableChannel;

import static infra.remoting.util.PromiseAdapter.adapt;

/**
 * An implementation of {@link Closeable} that wraps a {@link DisposableChannel}, enabling
 * close-ability and exposing the {@link DisposableChannel}'s address.
 */
public final class CloseableChannel implements Closeable {

  private final Channel channel;

  /**
   * Creates a new instance
   *
   * @param channel the {@link DisposableChannel} to wrap
   * @throws NullPointerException if {@code context} is {@code null}
   */
  CloseableChannel(DisposableChannel channel) {
    this.channel = Objects.requireNonNull(channel, "channel is required").channel();
  }

  /**
   * Creates a new instance
   *
   * @param channel the {@link DisposableChannel} to wrap
   * @throws NullPointerException if {@code context} is {@code null}
   */
  public CloseableChannel(Channel channel) {
    this.channel = Objects.requireNonNull(channel, "channel is required");
  }

  /**
   * Return local server selector channel address.
   *
   * @return local {@link InetSocketAddress}
   * @see DisposableChannel#address()
   */
  public InetSocketAddress address() {
    return (InetSocketAddress) channel.localAddress();
  }

  @Override
  public void dispose() {
    channel.close();
  }

  @Override
  public boolean isDisposed() {
    return !channel.isActive();
  }

  @Override
  public Mono<Void> onClose() {
    return FutureMono.of(adapt(channel.closeFuture()));
  }

}
