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

package infra.cloud.protocol;

import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import infra.cloud.RpcRequest;
import infra.core.AttributeAccessorSupport;
import infra.core.style.ToStringBuilder;
import infra.lang.Assert;
import infra.lang.Nullable;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.AttributeKey;
import io.netty.util.AttributeMap;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/12/22 23:37
 */
public class Connection extends AttributeAccessorSupport {

  static final AttributeKey<Connection> KEY = AttributeKey.valueOf(Connection.class, "Connection");

  private final AtomicInteger requestIdAddr = new AtomicInteger(0);

  private final ConcurrentHashMap<Integer, ResponsePromise> promiseMap = new ConcurrentHashMap<>();

  private final Channel channel;

  Connection(Channel channel) {
    this.channel = channel;
    channel.attr(KEY).set(this);
  }

  @Nullable
  public ResponsePromise getPromise(int requestId) {
    return promiseMap.get(requestId);
  }

  @Nullable
  public ResponsePromise getAndRemovePromise(int requestId) {
    return promiseMap.remove(requestId);
  }

  public ChannelFuture writeAndFlush(Object msg) {
    return channel.writeAndFlush(msg);
  }

  public ResponsePromise newPromise(RpcRequest rpcRequest) {
    int requestId = requestIdAddr.getAndIncrement();
    ResponsePromise responsePromise = new ResponsePromise(requestId, channel.eventLoop(), rpcRequest);
    promiseMap.put(requestId, responsePromise);
    return responsePromise;
  }

  public ByteBufAllocator alloc() {
    return channel.alloc();
  }

  public boolean isActive() {
    return channel.isActive();
  }

  public void disconnect() {
    channel.disconnect();
    for (ResponsePromise promise : promiseMap.values()) {
      promise.tryFailure(new ClosedChannelException());
    }
  }

  @Nullable
  public InetSocketAddress remoteAddress() {
    return (InetSocketAddress) channel.remoteAddress();
  }

  void channelInactive() {
    // todo 外部或许可以注册监听器，断开连接后回调
    for (ResponsePromise promise : promiseMap.values()) {
      promise.tryFailure(new ClosedChannelException());
    }
  }

  @Override
  public String toString() {
    return ToStringBuilder.forInstance(this)
            .append("channel", channel)
            .toString();
  }

  @Override
  public boolean equals(Object param) {
    if (this == param)
      return true;
    if (!(param instanceof Connection that))
      return false;
    if (!super.equals(param))
      return false;
    return Objects.equals(channel, that.channel);
  }

  @Override
  public int hashCode() {
    return Objects.hash(channel);
  }

  @Nullable
  public static Connection find(AttributeMap map) {
    return map.attr(KEY).get();
  }

  public static Connection obtain(AttributeMap map) {
    Connection connection = find(map);
    Assert.state(connection != null, "Connection not set");
    return connection;
  }

}
