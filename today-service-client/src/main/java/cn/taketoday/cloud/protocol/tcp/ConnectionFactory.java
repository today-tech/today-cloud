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

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import cn.taketoday.cloud.RpcResponse;
import cn.taketoday.cloud.core.serialize.Serialization;
import cn.taketoday.cloud.protocol.ProtocolPayload;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/1/7 16:56
 */
class ConnectionFactory extends BasePooledObjectFactory<Connection> {
  private static final Logger log = LoggerFactory.getLogger(ConnectionFactory.class);

  private final String host;

  private final int port;

  private final Serialization<RpcResponse> serialization;

  private final Bootstrap bootstrap = new Bootstrap();

  public ConnectionFactory(String host, int port, int ioThreadCount, Serialization<RpcResponse> serialization) {
    this.host = host;
    this.port = port;
    this.serialization = serialization;
    bootstrap.group(new NioEventLoopGroup(ioThreadCount, new DefaultThreadFactory("client")))
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<>() {
              @Override
              protected void initChannel(Channel ch) {
                ch.pipeline().addLast("frame-handler", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,
                                0, 4, 0, 4))
                        .addLast("channel-handler", new ResponseHandler());
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
    return p.getObject().channel.isActive();
  }

  @Override
  public void destroyObject(PooledObject<Connection> p) throws Exception {
    p.getObject().channel.close();
  }

  class ResponseHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      log.debug("connected");
      ctx.fireChannelActive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      cause.printStackTrace();
      ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      if (msg instanceof ByteBuf byteBuf) {
        ProtocolPayload payload = ProtocolPayload.decode(byteBuf);
        Connection connection = Connection.obtain(ctx);
        ResponsePromise responsePromise = connection.getAndRemovePromise(payload.getRequestId());
        try {
          if (payload.body != null) {
            RpcResponse response = serialization.deserialize(new ByteArrayInputStream(payload.body));
            Throwable exception = response.getException();
            if (exception != null) {
              responsePromise.setFailure(exception);
            }
            else {
              responsePromise.setSuccess(response.getResult());
            }
          }
          else {
            responsePromise.setSuccess(null);
          }
        }
        catch (IOException e) {
          responsePromise.setFailure(e);
        }
      }
      else {
        ctx.fireChannelRead(msg);
      }
    }

  }

}
