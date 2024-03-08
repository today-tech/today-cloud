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

package cn.taketoday.cloud.provider;

import java.util.List;
import java.util.concurrent.Executor;

import cn.taketoday.cloud.netty.ChannelConnector;
import cn.taketoday.cloud.protocol.EventHandler;
import cn.taketoday.cloud.protocol.ProtocolPayload;
import cn.taketoday.cloud.protocol.RemoteEventType;
import cn.taketoday.context.SmartLifecycle;
import cn.taketoday.util.MultiValueMap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/11/22 22:35
 */
public class ServiceProviderChannelConnector extends ChannelConnector implements SmartLifecycle {

  private volatile boolean running;

  private final Executor eventAsyncExecutor;

  private final MultiValueMap<RemoteEventType, EventHandler> eventHandlers;

  ServiceProviderChannelConnector(Executor eventAsyncExecutor, List<EventHandler> handlers) {
    this.eventAsyncExecutor = eventAsyncExecutor;
    this.eventHandlers = MultiValueMap.forSmartListAdaption();
    for (EventHandler handler : handlers) {
      for (RemoteEventType eventType : handler.getSupportedEvents()) {
        eventHandlers.add(eventType, handler);
      }
    }
  }

  @Override
  protected void onActive(ChannelHandlerContext ctx) throws Exception {
    logger.debug("Client connected");
  }

  @Override
  protected void onInactive(ChannelHandlerContext ctx) throws Exception {
    logger.debug("Client disconnected");
  }

  @Override
  protected void initChannel(Channel ch, ChannelPipeline pipeline) throws Exception {
    pipeline.addLast("tcp-frame-decoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,
            0, 4, 0, 4));
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    logger.warn("exception caught", cause);
    ctx.close();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof ByteBuf byteBuf) {
      ProtocolPayload payload = ProtocolPayload.parse(byteBuf);
      try {
        handleEvent(ctx, payload);
      }
      finally {
        byteBuf.release();
      }
    }
    else {
      ctx.fireChannelRead(msg);
    }
  }

  private void handleEvent(ChannelHandlerContext ctx, ProtocolPayload payload) throws Exception {
    RemoteEventType eventType = payload.getEventType();
    List<EventHandler> handlers = eventHandlers.get(eventType);
    if (handlers != null) {
      for (EventHandler handler : handlers) {
        if (handler.supportsAsync()) {
          eventAsyncExecutor.execute(() -> invokeHandler(handler, ctx, payload));
        }
        else {
          invokeHandler(handler, ctx, payload);
        }
      }
    }
    else {
      logger.debug("Not supported event type: [{}]", eventType);
    }
  }

  private void invokeHandler(EventHandler handler, ChannelHandlerContext ctx, ProtocolPayload payload) {
    try {
      handler.handleEvent(ctx.channel(), payload);
    }
    catch (Exception e) {
      // TODO exception handling
      throw new RuntimeException(e);
    }
  }

  @Override
  public void start() {
    bind();
    running = true;
  }

  @Override
  public void stop() {
    shutdown();
    running = false;
  }

  @Override
  public boolean isRunning() {
    return running;
  }

}
