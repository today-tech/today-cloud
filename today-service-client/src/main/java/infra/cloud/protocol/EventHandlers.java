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

import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.Executor;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.MultiValueMap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/9/13 21:00
 */
public class EventHandlers extends ChannelInboundHandlerAdapter {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private final Executor eventAsyncExecutor;

  private final MultiValueMap<RemoteEventType, EventHandler> eventHandlers;

  public EventHandlers(Executor eventAsyncExecutor, List<EventHandler> handlers) {
    this.eventHandlers = MultiValueMap.forSmartListAdaption(new EnumMap<>(RemoteEventType.class));
    this.eventAsyncExecutor = eventAsyncExecutor;
    for (EventHandler handler : handlers) {
      for (RemoteEventType eventType : handler.getSupportedEvents()) {
        eventHandlers.add(eventType, handler);
      }
    }
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof ByteBuf byteBuf) {
      ProtocolPayload payload = ProtocolPayload.parse(byteBuf);
      handleEvent(ctx.channel(), payload);
    }
    else {
      ctx.fireChannelRead(msg);
    }
  }

  protected final void handleEvent(Channel channel, ProtocolPayload payload) {
    RemoteEventType eventType = payload.getEventType();
    List<EventHandler> handlers = eventHandlers.get(eventType);
    if (handlers != null) {
      for (EventHandler handler : handlers) {
        if (handler.supportsAsync()) {
          eventAsyncExecutor.execute(() -> invokeHandler(handler, channel, payload));
        }
        else {
          invokeHandler(handler, channel, payload);
        }
      }
    }
    else {
      logger.debug("Not supported event type: [{}]", eventType);
    }
  }

  private void invokeHandler(EventHandler handler, Channel channel, ProtocolPayload payload) {
    try {
      handler.handleEvent(channel, payload);
    }
    catch (Exception e) {
      // TODO exception handling
      throw new RuntimeException(e);
    }
    finally {
      payload.release();
    }
  }

}
