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

package infra.cloud.provider;

import infra.cloud.netty.ChannelConnector;
import infra.cloud.protocol.EventHandlers;
import infra.context.SmartLifecycle;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/11/22 22:35
 */
public class ServiceProviderChannelConnector extends ChannelConnector implements SmartLifecycle {

  private volatile boolean running;

  private final EventHandlers eventHandlers;

  public ServiceProviderChannelConnector(EventHandlers eventHandlers) {
    this.eventHandlers = eventHandlers;
  }

  protected void onActive(ChannelHandlerContext ctx) {
    logger.debug("Client connected");
  }

  protected void onInactive(ChannelHandlerContext ctx) {
    logger.debug("Client disconnected");
  }

  @Override
  protected void initChannel(Channel ch, ChannelPipeline pipeline) {
    pipeline.addLast("tcp-frame-decoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,
            0, 4, 0, 4));
  }

  @Override
  protected ChannelHandler getChannelHandler() {
    return eventHandlers;
  }

  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    logger.warn("exception caught", cause);
    ctx.close();
  }

  @Override
  public void start() {
    bind();
    running = true;
  }

  @Override
  public void stop() {
    shutdown().awaitUninterruptibly();
    running = false;
  }

  @Override
  public void stop(Runnable callback) {
    shutdown().onFinally(() -> {
      callback.run();
      running = false;
    });
  }

  @Override
  public boolean isRunning() {
    return running;
  }

}
