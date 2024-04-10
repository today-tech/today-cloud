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

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.cloud.RpcRequest;
import cn.taketoday.cloud.RpcResponse;
import cn.taketoday.cloud.ServiceInstance;
import cn.taketoday.cloud.core.serialize.ProtostuffUtils;
import cn.taketoday.cloud.core.serialize.Serialization;
import cn.taketoday.cloud.protocol.PayloadHeader;
import cn.taketoday.cloud.protocol.ProtocolPayload;
import cn.taketoday.cloud.protocol.RemoteEventType;
import cn.taketoday.util.ExceptionUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/1/7 16:53
 */
class NettyClient {

  private final Serialization<RpcResponse> serialization;

  private final ConcurrentHashMap<HostKey, GenericObjectPool<Connection>> channelMap = new ConcurrentHashMap<>();

  private int ioThreadCount = 4;

  public NettyClient(Serialization<RpcResponse> serialization) {
    this.serialization = serialization;
  }

  public void setIoThreadCount(int ioThreadCount) {
    this.ioThreadCount = ioThreadCount;
  }

  public ResponsePromise write(ServiceInstance selected, RpcRequest rpcRequest, Duration requestTimeout) {
    HostKey hostKey = new HostKey(selected);
    GenericObjectPool<Connection> connectionPool = channelMap.get(hostKey);
    if (connectionPool == null) {
      var factory = new ConnectionFactory(hostKey.host, hostKey.port, ioThreadCount);
      var config = createPoolConfig();
      connectionPool = new GenericObjectPool<>(factory, config);
      channelMap.put(hostKey, connectionPool);
    }

    Connection connection = null;
    try {
      connection = connectionPool.borrowObject(requestTimeout);
      ResponsePromise responsePromise = connection.newPromise();

      ByteBuf payload = createPayload(rpcRequest, connection, responsePromise);
      connection.writeAndFlush(payload);

      return responsePromise;
    }
    catch (Exception e) {
      throw ExceptionUtils.sneakyThrow(e);
    }
    finally {
      if (connection != null) {
        connectionPool.returnObject(connection);
      }
    }
  }

  private ByteBuf createPayload(RpcRequest rpcRequest, Connection connection, ResponsePromise responsePromise) {
    byte[] body = ProtostuffUtils.serialize(rpcRequest);

    ByteBufAllocator allocator = connection.channel.alloc();
    ByteBuf payload = allocator.buffer(4 + ProtocolPayload.HEADER_LENGTH + body.length);
    payload.writeInt(body.length + ProtocolPayload.HEADER_LENGTH);

    PayloadHeader.serialize(payload, responsePromise.getRequestId(), RemoteEventType.RPC_INVOCATION);

    payload.writeBytes(body);
    return payload;
  }

  protected GenericObjectPoolConfig<Connection> createPoolConfig() {
    var config = new GenericObjectPoolConfig<Connection>();
    config.setMinIdle(20);
    config.setMaxTotal(20);
    config.setMaxIdle(20);
    config.setTestOnBorrow(true);
    config.setTestWhileIdle(true);
    return config;
  }

  static class HostKey {
    final String host;

    final int port;

    HostKey(ServiceInstance selected) {
      this.host = selected.getHost();
      this.port = selected.getPort();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof HostKey hostKey))
        return false;
      return port == hostKey.port && Objects.equals(host, hostKey.host);
    }

    @Override
    public int hashCode() {
      return Objects.hash(host, port);
    }

  }
}
