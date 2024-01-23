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

package cn.taketoday.cloud.protocol.tcp;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.AttributeKey;
import io.netty.util.AttributeMap;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/12/22 23:37
 */
public class Connection {
  static final AttributeKey<Connection> KEY = AttributeKey.valueOf(Connection.class, "Connection");

  private final AtomicInteger requestIdAddr = new AtomicInteger(0);

  final Channel channel;

  private final ConcurrentHashMap<Integer, ResponsePromise> promiseMap = new ConcurrentHashMap<>();

  Connection(Channel channel) {
    this.channel = channel;
    channel.attr(KEY).set(this);
  }

  public ResponsePromise getPromise(int requestId) {
    return promiseMap.get(requestId);
  }

  public ResponsePromise getAndRemovePromise(int requestId) {
    return promiseMap.remove(requestId);
  }

  public ChannelFuture writeAndFlush(Object msg) {
    return channel.writeAndFlush(msg);
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

  public ResponsePromise newPromise() {
    int requestId = requestIdAddr.getAndIncrement();
    ResponsePromise responsePromise = new ResponsePromise(requestId, channel.eventLoop());
    promiseMap.put(requestId, responsePromise);
    return responsePromise;
  }

}
