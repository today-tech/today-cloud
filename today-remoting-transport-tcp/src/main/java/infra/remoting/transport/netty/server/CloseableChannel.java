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

package infra.remoting.transport.netty.server;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Objects;

import io.netty.channel.Channel;
import io.rsocket.Closeable;
import io.rsocket.util.FutureMono;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableChannel;

import static infra.remoting.transport.tcp.PromiseAdapter.adapt;

/**
 * An implementation of {@link Closeable} that wraps a {@link DisposableChannel}, enabling
 * close-ability and exposing the {@link DisposableChannel}'s address.
 */
public final class CloseableChannel implements Closeable {

  /** For forward compatibility: remove when RSocket compiles against Reactor 1.0. */
  private static final Method channelAddressMethod;

  static {
    try {
      channelAddressMethod = DisposableChannel.class.getMethod("address");
    }
    catch (NoSuchMethodException ex) {
      throw new IllegalStateException("Expected address method", ex);
    }
  }

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
    try {
      return (InetSocketAddress) channel.localAddress();
    }
    catch (ClassCastException | NoSuchMethodError e) {
      try {
        return (InetSocketAddress) channelAddressMethod.invoke(this.channel);
      }
      catch (Exception ex) {
        throw new IllegalStateException("Unable to obtain address", ex);
      }
    }
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
