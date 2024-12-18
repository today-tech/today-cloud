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

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/1/7 16:56
 */
public class ConnectionFactory extends BasePooledObjectFactory<Connection> {

  private final String host;

  private final int port;

  private final Bootstrap bootstrap = new Bootstrap();

  public ConnectionFactory(String host, int port, int ioThreadCount, ChannelHandler channelHandler) {
    this.host = host;
    this.port = port;
    bootstrap.group(new NioEventLoopGroup(ioThreadCount, new DefaultThreadFactory("client")))
            .channel(NioSocketChannel.class)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
            .handler(new ChannelInitializer<>() {
              @Override
              protected void initChannel(Channel ch) {
                ch.pipeline().addLast("frame-handler", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,
                                0, 4, 0, 4))
                        .addLast("channel-handler", channelHandler);
              }
            });
  }

  @Override
  public Connection create() throws Exception {
    Channel channel = bootstrap.connect(host, port).sync().channel();
    return new Connection(channel);
  }

  @Override
  public PooledObject<Connection> wrap(Connection connection) {
    return new DefaultPooledObject<>(connection);
  }

  /**
   * Ensures that the instance is safe to be returned by the pool.
   *
   * @param p a {@code PooledObject} wrapping the instance to be validated
   * @return {@code false} if {@code obj} is not valid and should
   * be dropped from the pool, {@code true} otherwise.
   */
  @Override
  public boolean validateObject(PooledObject<Connection> p) {
    return p.getObject().isActive();
  }

  @Override
  public void destroyObject(PooledObject<Connection> p) throws Exception {
    p.getObject().disconnect();
  }

}
