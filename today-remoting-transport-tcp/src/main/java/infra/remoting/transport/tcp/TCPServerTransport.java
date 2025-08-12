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

package infra.remoting.transport.tcp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import infra.core.FutureMono;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.remoting.transport.ConnectionAcceptor;
import infra.remoting.transport.ServerTransport;
import infra.remoting.transport.netty.server.CloseableChannel;
import infra.util.ClassUtils;
import infra.util.concurrent.Future;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.NetUtil;
import io.netty.util.concurrent.DefaultThreadFactory;
import reactor.core.publisher.Mono;

import static infra.remoting.util.PromiseAdapter.adapt;

/**
 * Netty TCP server transport
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/7/28 19:54
 */
public class TCPServerTransport extends ChannelInboundHandlerAdapter implements ServerTransport<CloseableChannel> {

  public static final int FRAME_LENGTH_SIZE = 3;

  private static final boolean epollPresent = ClassUtils.isPresent(
          "io.netty.channel.epoll.EpollServerSocketChannel", TCPServerTransport.class);

  private static final Logger log = LoggerFactory.getLogger(TCPServerTransport.class);

  private final int maxFrameLength;

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
  private InetAddress listenAddress;

  private int port;

  @Nullable
  protected Channel channel;

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

  public void setListenAddress(@Nullable InetAddress listenAddress) {
    this.listenAddress = listenAddress;
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

  public TCPServerTransport(int maxFrameLength) {
    this.maxFrameLength = maxFrameLength;
  }

  @Override
  public int getMaxFrameLength() {
    return maxFrameLength;
  }

  @Override
  public Mono<CloseableChannel> start(ConnectionAcceptor acceptor) {
    return FutureMono.of(bind(acceptor));
  }

  //  @Override
  public Future<CloseableChannel> bind(ConnectionAcceptor acceptor) {
    ServerBootstrap bootstrap = new ServerBootstrap();
    preBootstrap(bootstrap);

    // enable epoll
    if (epollIsAvailable()) {
      EpollDelegate.init(this);
    }
    else {
      IoHandlerFactory ioHandlerFactory = NioIoHandler.newFactory();
      if (acceptorGroup == null) {
        acceptorGroup = new MultiThreadIoEventLoopGroup(acceptorThreadCount,
                new DefaultThreadFactory("acceptor"), ioHandlerFactory);
      }
      if (workerGroup == null) {
        workerGroup = new MultiThreadIoEventLoopGroup(workerThreadCount,
                new DefaultThreadFactory("workers"), ioHandlerFactory);
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

    bootstrap.childHandler(new ChannelConnectorInitializer(this, acceptor));
    bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

    postBootstrap(bootstrap);

    ChannelFuture channelFuture = bootstrap.bind(getListenAddress(port));
    this.channel = channelFuture.channel();
    Assert.state(channel != null, "channel is null");
    return adapt(channelFuture).map(v -> new CloseableChannel(channel)).onCompleted(future -> {
      if (future.isSuccess()) {
        log.info("Netty started on port: '{}'", getPort());
      }
      else {
        log.error("{} failed to start", this, future.getCause());
      }
    });
  }

  public Future<Void> shutdown() {
    log.info("Shutdown netty: [{}] on port: '{}'", this, getPort());
    return Future.combine(channel == null ? Future.ok() : adapt(channel.close()))
            .with(shutdownEventLoop(acceptorGroup), shutdownEventLoop(workerGroup))
            .asVoid();
  }

  private Future<?> shutdownEventLoop(@Nullable EventLoopGroup group) {
    if (group == null) {
      return Future.ok();
    }
    return adapt(group.shutdownGracefully(1, 10, TimeUnit.SECONDS));
  }

  private InetSocketAddress getListenAddress(int port) {
    if (listenAddress != null) {
      return new InetSocketAddress(listenAddress.getHostAddress(), port);
    }
    return new InetSocketAddress(port);
  }

  /**
   * before bootstrap
   *
   * @param bootstrap netty ServerBootstrap
   */
  protected void preBootstrap(ServerBootstrap bootstrap) {
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

    private final TCPServerTransport transport;

    private final ConnectionAcceptor acceptor;

    ChannelConnectorInitializer(TCPServerTransport transport, ConnectionAcceptor acceptor) {
      this.transport = transport;
      this.acceptor = acceptor;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
      ch.pipeline().addLast("tcp-frame-decoder", new LengthFieldBasedFrameDecoder(transport.getMaxFrameLength(),
                      0, FRAME_LENGTH_SIZE, 0, FRAME_LENGTH_SIZE))
              .addLast("channel-transport", transport);

//      acceptor.accept(new TcpConnection(ch));
    }
  }

  static class EpollDelegate {
    static void init(TCPServerTransport transport) {
      if (transport.socketChannel == null) {
        transport.setSocketChannel(EpollServerSocketChannel.class);
      }
      IoHandlerFactory ioHandlerFactory = EpollIoHandler.newFactory();
      if (transport.acceptorGroup == null) {
        transport.setAcceptorGroup(new MultiThreadIoEventLoopGroup(
                transport.acceptorThreadCount, new DefaultThreadFactory("epoll-acceptor"), ioHandlerFactory));
      }
      if (transport.workerGroup == null) {
        transport.setWorkerGroup(new MultiThreadIoEventLoopGroup(
                transport.workerThreadCount, new DefaultThreadFactory("epoll-workers"), ioHandlerFactory));
      }
    }
  }
}
