/*
 * Copyright 2021 - 2023 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.cloud.RpcRequest;
import cn.taketoday.cloud.RpcResponse;
import cn.taketoday.cloud.ServiceInstance;
import cn.taketoday.cloud.ServiceMethodInvoker;
import cn.taketoday.cloud.ServiceTimeoutException;
import cn.taketoday.cloud.core.serialize.Serialization;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.Promise;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/12/1 20:42
 */
public class TcpServiceMethodInvoker extends ServiceMethodInvoker {
  static final AttributeKey<Promise<RpcResponse>> RpcResponsePromise = AttributeKey.valueOf("RpcResponse");

  private static final Logger log = LoggerFactory.getLogger(TcpServiceMethodInvoker.class);

  private final Serialization<RpcResponse> serialization;

  final Client client = new Client();

  private Duration requestTimeout = Duration.ofSeconds(10);

  public TcpServiceMethodInvoker(Serialization<RpcResponse> serialization) {
    this.serialization = serialization;
  }

  public void setRequestTimeout(Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
  }

  @Override
  protected RpcResponse invokeInternal(ServiceInstance selected, Method method, Object[] args) throws IOException {
    RpcRequest rpcRequest = new RpcRequest();
    rpcRequest.setMethod(method.getName());
    rpcRequest.setServiceName(selected.getServiceId());
    rpcRequest.setParameterTypes(method.getParameterTypes());
    rpcRequest.setArguments(args);

    Connection connection = client.connect(selected);
    Promise<RpcResponse> responsePromise = connection.write(rpcRequest);

    try {
      responsePromise.await(requestTimeout.toMillis());
    }
    catch (InterruptedException e) {
      // FIXME
      Thread.interrupted();
      throw new RuntimeException(e);
    }

    if (responsePromise.isSuccess()) {
      return responsePromise.getNow();
    }

    if (!responsePromise.isDone()) {
      throw new ServiceTimeoutException("Remoting invoke timeout", null);
    }

    Throwable cause = responsePromise.cause();
    throw new ServiceTimeoutException("Remoting invoke failed", cause);
  }

  class Handler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      log.debug("connected");
      ctx.fireChannelActive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      if (msg instanceof ByteBuf byteBuf) {
//        log.debug("channelRead {}", byteBuf.readableBytes());
        Promise<RpcResponse> responsePromise = ctx.channel().attr(RpcResponsePromise).get();
        try (ByteBufInputStream stream = new ByteBufInputStream(byteBuf)) {
          RpcResponse response = serialization.deserialize(stream);
          responsePromise.setSuccess(response);
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

  class Connection {

    final Channel channel;

    Connection(Channel channel) {
      this.channel = channel;
    }

    Promise<RpcResponse> write(RpcRequest rpcRequest) throws IOException {
      ByteArrayOutputStream output = new ByteArrayOutputStream(100);
      serialization.serialize(rpcRequest, output);

      byte[] writeBuffer = new byte[4];
      byte[] body = output.toByteArray();
      int length = body.length;

      writeBuffer[0] = (byte) (length >>> 24);
      writeBuffer[1] = (byte) (length >>> 16);
      writeBuffer[2] = (byte) (length >>> 8);
      writeBuffer[3] = (byte) (length >>> 0);

      ByteBuf msg = Unpooled.wrappedBuffer(writeBuffer, body);
      channel.writeAndFlush(msg);

      Promise<RpcResponse> responsePromise = channel.eventLoop().newPromise();
      channel.attr(RpcResponsePromise).set(responsePromise);
      return responsePromise;
    }

  }

  class Client {

    private int ioThreadCount = 4;

    private final ConcurrentHashMap<Key, Connection> channelMap = new ConcurrentHashMap<>();

    public void setIoThreadCount(int ioThreadCount) {
      this.ioThreadCount = ioThreadCount;
    }

    Connection connect(ServiceInstance selected) {
      Key key = new Key(selected);
      Connection connection = channelMap.get(key);
      if (connection == null || !connection.channel.isActive()) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup(ioThreadCount, new DefaultThreadFactory("client")))
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<>() {
                  @Override
                  protected void initChannel(Channel ch) {
                    ch.pipeline().addLast("frame-handler", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,
                                    0, 4, 0, 4))
                            .addLast("channel-handler", new Handler());
                  }
                });

        Channel channel = bootstrap.connect(key.host, key.port).syncUninterruptibly().channel();
        connection = new Connection(channel);
        channelMap.put(key, connection);
      }
      return connection;
    }

  }

  static class Key {
    private final String host;

    private final int port;

    Key(ServiceInstance selected) {
      this.host = selected.getHost();
      this.port = selected.getPort();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof Key key))
        return false;
      return port == key.port && Objects.equals(host, key.host);
    }

    @Override
    public int hashCode() {
      return Objects.hash(host, port);
    }

  }

}
