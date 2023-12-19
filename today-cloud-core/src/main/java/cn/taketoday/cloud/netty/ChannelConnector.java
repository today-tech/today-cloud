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

package cn.taketoday.cloud.netty;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.NetUtil;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * Netty channel connector
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/11/22 21:40
 */
public abstract class ChannelConnector extends ChannelInboundHandlerAdapter {

  static final boolean epollPresent = ClassUtils.isPresent(
          "io.netty.channel.epoll.EpollServerSocketChannel", ChannelConnector.class);

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * the number of threads that will be used by
   * {@link io.netty.util.concurrent.MultithreadEventExecutorGroup}
   *
   * For child {@link EventLoopGroup}
   *
   * @see io.netty.util.concurrent.MultithreadEventExecutorGroup
   */
  private int workerThreadCount = 4;

  /**
   * the number of threads that will be used by
   * {@link io.netty.util.concurrent.MultithreadEventExecutorGroup}
   *
   * For parent {@link EventLoopGroup}
   *
   * @see io.netty.util.concurrent.MultithreadEventExecutorGroup
   */
  private int acceptorThreadCount = 2;

  /**
   * The SOMAXCONN value of the current machine.  If failed to get the value,  {@code 200} is used as a
   * default value for Windows and {@code 128} for others.
   * <p>
   * so_backlog
   */
  private int maxConnection = NetUtil.SOMAXCONN;

  @Nullable
  private EventLoopGroup workerGroup;

  @Nullable
  private EventLoopGroup acceptorGroup;

  @Nullable
  private Class<? extends ServerSocketChannel> socketChannel;

  @Nullable
  private LogLevel loggingLevel;

  @Nullable
  private InetAddress address;

  private int port;

  /**
   * EventLoopGroup for acceptor
   *
   * @param acceptorGroup acceptor
   */
  public void setAcceptorGroup(@Nullable EventLoopGroup acceptorGroup) {
    this.acceptorGroup = acceptorGroup;
  }

  /**
   * set the worker EventLoopGroup
   *
   * @param workerGroup worker
   */
  public void setWorkerGroup(@Nullable EventLoopGroup workerGroup) {
    this.workerGroup = workerGroup;
  }

  public void setSocketChannel(@Nullable Class<? extends ServerSocketChannel> socketChannel) {
    this.socketChannel = socketChannel;
  }

  /**
   * set the number of threads that will be used by
   * {@link io.netty.util.concurrent.MultithreadEventExecutorGroup}
   *
   * For parent {@link EventLoopGroup}
   *
   * @see io.netty.util.concurrent.MultithreadEventExecutorGroup
   */
  public void setAcceptorThreadCount(int acceptorThreadCount) {
    this.acceptorThreadCount = acceptorThreadCount;
  }

  /**
   * get the number of threads that will be used by
   * {@link io.netty.util.concurrent.MultithreadEventExecutorGroup}
   *
   * For parent {@link EventLoopGroup}
   *
   * @see io.netty.util.concurrent.MultithreadEventExecutorGroup
   */
  public int getAcceptorThreadCount() {
    return acceptorThreadCount;
  }

  /**
   * set the number of threads that will be used by
   * {@link io.netty.util.concurrent.MultithreadEventExecutorGroup}
   *
   * For child {@link EventLoopGroup}
   *
   * @see io.netty.util.concurrent.MultithreadEventExecutorGroup
   */
  public void setWorkerThreadCount(int workThreadCount) {
    this.workerThreadCount = workThreadCount;
  }

  /**
   * The SOMAXCONN value of the current machine.  If failed to get the value,  {@code 200} is used as a
   * default value for Windows and {@code 128} for others.
   * <p>
   * so_backlog
   */
  public void setMaxConnection(int maxConnection) {
    this.maxConnection = maxConnection;
  }

  /**
   * get the number of threads that will be used by
   * {@link io.netty.util.concurrent.MultithreadEventExecutorGroup}
   *
   * For child {@link EventLoopGroup}
   *
   * @see io.netty.util.concurrent.MultithreadEventExecutorGroup
   */
  public int getWorkThreadCount() {
    return workerThreadCount;
  }

  /**
   * Return the address that the web server binds to.
   *
   * @return the address
   */
  @Nullable
  public InetAddress getAddress() {
    return this.address;
  }

  public void setAddress(@Nullable InetAddress address) {
    this.address = address;
  }

  /**
   * The port that the web server listens on.
   *
   * @return the port
   */
  public int getPort() {
    return this.port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  /**
   * Set {@link LoggingHandler} logging Level
   * <p>
   * If that {@code loggingLevel} is {@code null} will not register logging handler
   * </p>
   *
   * @param loggingLevel LogLevel
   * @see LogLevel
   * @see LoggingHandler
   */
  public void setLoggingLevel(@Nullable LogLevel loggingLevel) {
    this.loggingLevel = loggingLevel;
  }

  public void bind() {
    ServerBootstrap bootstrap = new ServerBootstrap();
    preBootstrap(bootstrap);

    // enable epoll
    if (epollIsAvailable()) {
      EpollDelegate.init(this);
    }
    else {
      if (acceptorGroup == null) {
        acceptorGroup = new NioEventLoopGroup(acceptorThreadCount, new DefaultThreadFactory("acceptor"));
      }
      if (workerGroup == null) {
        workerGroup = new NioEventLoopGroup(workerThreadCount, new DefaultThreadFactory("workers"));
      }
      if (socketChannel == null) {
        socketChannel = NioServerSocketChannel.class;
      }
    }

    Assert.state(workerGroup != null, "No 'workerGroup'");
    Assert.state(acceptorGroup != null, "No 'acceptorGroup'");

    bootstrap.group(acceptorGroup, workerGroup);
    bootstrap.channel(socketChannel);
    bootstrap.option(ChannelOption.SO_BACKLOG, maxConnection);

    if (loggingLevel != null) {
      bootstrap.handler(new LoggingHandler(loggingLevel));
    }

    bootstrap.childHandler(new ChannelConnectorInitializer(this));
    bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

    postBootstrap(bootstrap);

    InetSocketAddress listenAddress = getListenAddress();
    bootstrap.bind(listenAddress).syncUninterruptibly();

    logger.info("Netty started on port: '{}'", getPort());
  }

  @Override
  public final void channelActive(ChannelHandlerContext ctx) throws Exception {
    try {
      onActive(ctx);
    }
    finally {
      ctx.fireChannelActive();
    }
  }

  @Override
  public final void channelInactive(ChannelHandlerContext ctx) throws Exception {
    try {
      onInactive(ctx);
    }
    finally {
      ctx.fireChannelActive();
    }
  }

  protected void onActive(ChannelHandlerContext ctx) throws Exception {

  }

  protected void onInactive(ChannelHandlerContext ctx) throws Exception {

  }

  protected abstract void initChannel(Channel ch, ChannelPipeline pipeline) throws Exception;

  public void shutdown() {
    logger.info("Shutdown netty: [{}] on port: '{}'", this, getPort());

    if (acceptorGroup != null) {
      acceptorGroup.shutdownGracefully();
    }
    if (workerGroup != null) {
      workerGroup.shutdownGracefully();
    }
  }

  private InetSocketAddress getListenAddress() {
    if (getAddress() != null) {
      return new InetSocketAddress(getAddress().getHostAddress(), getPort());
    }
    return new InetSocketAddress(getPort());
  }

  @Override
  public boolean isSharable() {
    return true;
  }

  /**
   * before bootstrap
   *
   * @param bootstrap netty ServerBootstrap
   */
  protected void preBootstrap(ServerBootstrap bootstrap) {
    ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
  }

  /**
   * after bootstrap
   *
   * @param bootstrap netty ServerBootstrap
   */
  protected void postBootstrap(ServerBootstrap bootstrap) {

  }

  /**
   * Subclasses can override this method to perform epoll is available logic
   */
  protected boolean epollIsAvailable() {
    return epollPresent && Epoll.isAvailable();
  }

  static class ChannelConnectorInitializer extends ChannelInitializer<Channel> {
    final ChannelConnector connector;

    ChannelConnectorInitializer(ChannelConnector connector) { this.connector = connector; }

    @Override
    protected void initChannel(Channel ch) throws Exception {
      ChannelPipeline pipeline = ch.pipeline();
      connector.initChannel(ch, pipeline);

      pipeline.addLast("channel-connector", connector);
    }
  }

  static class EpollDelegate {
    static void init(ChannelConnector connector) {
      if (connector.socketChannel == null) {
        connector.setSocketChannel(EpollServerSocketChannel.class);
      }
      if (connector.acceptorGroup == null) {
        connector.setAcceptorGroup(new EpollEventLoopGroup(
                connector.acceptorThreadCount, new DefaultThreadFactory("epoll-acceptor")));
      }
      if (connector.workerGroup == null) {
        connector.setWorkerGroup(new EpollEventLoopGroup(
                connector.workerThreadCount, new DefaultThreadFactory("epoll-workers")));
      }
    }
  }
}
