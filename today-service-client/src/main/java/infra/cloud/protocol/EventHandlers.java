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
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.PromiseCombiner;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/9/13 21:00
 */
public class EventHandlers extends ChannelDuplexHandler {

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
  public void channelInactive(ChannelHandlerContext ctx) {
    Connection connection = Connection.find(ctx);
    if (connection != null) {
      connection.channelInactive();
    }
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    Connection connection = Connection.obtain(ctx);
    for (List<EventHandler> handlers : eventHandlers.values()) {
      for (EventHandler handler : handlers) {
        handler.channelActive(connection);
      }
    }
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof ByteBuf byteBuf) {
      Connection connection = Connection.obtain(ctx);
      ProtocolPayload payload = ProtocolPayload.parse(byteBuf);
      handleEvent(connection, payload);
      payload.release();
    }
    else {
      ctx.fireChannelRead(msg);
    }
  }

  protected final void handleEvent(Connection connection, ProtocolPayload payload) {
    RemoteEventType eventType = payload.getEventType();
    List<EventHandler> handlers = eventHandlers.get(eventType);
    if (handlers != null) {
      for (EventHandler handler : handlers) {
        if (handler.supportsAsync(eventType)) {
          eventAsyncExecutor.execute(() -> invokeHandler(handler, connection, payload));
        }
        else {
          invokeHandler(handler, connection, payload);
        }
      }
    }
    else {
      logger.debug("Not supported event type: [{}]", eventType);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    logger.error("Unexpected exception", cause);
  }

  private void invokeHandler(EventHandler handler, Connection connection, ProtocolPayload payload) {
    try {
      handler.handleEvent(connection, payload.retainedDuplicate());
    }
    catch (Exception e) {
      // TODO exception handling
      throw new RuntimeException(e);
    }
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
    if (msg instanceof ProtocolPayload payload) {
      PromiseCombiner combiner = new PromiseCombiner(ctx.executor());

      ByteBuf length = ctx.alloc().buffer(4).writeInt(payload.getLength());
      ByteBuf header = payload.header.serialize(ctx.alloc());

      combiner.add(ctx.write(length));
      combiner.add(ctx.write(header));
      if (payload.body != null) {
        combiner.add(ctx.write(payload.body));
      }
      combiner.finish(promise);
    }
    else {
      ctx.write(msg, promise);
    }
  }

}
