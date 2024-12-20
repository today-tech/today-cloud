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

package infra.cloud.protocol.tcp;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import infra.cloud.RpcRequest;
import infra.cloud.ServiceInstance;
import infra.cloud.protocol.ByteBufOutput;
import infra.cloud.protocol.Connection;
import infra.cloud.protocol.ConnectionFactory;
import infra.cloud.protocol.PayloadHeader;
import infra.cloud.protocol.ProtocolPayload;
import infra.cloud.protocol.RemoteEventType;
import infra.cloud.protocol.ResponsePromise;
import infra.cloud.serialize.RpcRequestSerialization;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/1/7 16:53
 */
class NettyClient {

  private final ConcurrentHashMap<HostKey, GenericObjectPool<Connection>> channelMap = new ConcurrentHashMap<>();

  private int ioThreadCount = 4;

  private final ChannelHandler channelHandler;

  // todo configurable
  private int initialCapacity = 64;

  private final RpcRequestSerialization rpcRequestSerialization;

  public NettyClient(ChannelHandler channelHandler, RpcRequestSerialization rpcRequestSerialization) {
    this.channelHandler = channelHandler;
    this.rpcRequestSerialization = rpcRequestSerialization;
  }

  public void setIoThreadCount(int ioThreadCount) {
    this.ioThreadCount = ioThreadCount;
  }

  public ResponsePromise write(ServiceInstance selected, RpcRequest rpcRequest, Duration requestTimeout) throws Exception {
    HostKey hostKey = new HostKey(selected);
    GenericObjectPool<Connection> connectionPool = channelMap.get(hostKey);
    if (connectionPool == null) {
      var factory = new ConnectionFactory(hostKey.host, hostKey.port, ioThreadCount, channelHandler);
      var config = createPoolConfig();
      connectionPool = new GenericObjectPool<>(factory, config);
      channelMap.put(hostKey, connectionPool);
    }

    Connection connection = null;
    try {
      connection = connectionPool.borrowObject(requestTimeout);
      ResponsePromise responsePromise = connection.newPromise();

      ProtocolPayload payload = createPayload(rpcRequest, connection, responsePromise);
      connection.writeAndFlush(payload);
      return responsePromise;
    }
    finally {
      if (connection != null) {
        connectionPool.returnObject(connection);
      }
    }
  }

  private ProtocolPayload createPayload(RpcRequest rpcRequest, Connection connection, ResponsePromise promise) throws IOException {
    ByteBuf body = connection.alloc().buffer(initialCapacity);
    PayloadHeader payloadHeader = PayloadHeader.forHeader(promise.getRequestId(), RemoteEventType.RPC_REQUEST);

    rpcRequestSerialization.serialize(rpcRequest, body, new ByteBufOutput(body));
    return new ProtocolPayload(payloadHeader, body);
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
