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

package infra.remoting.transport.netty.server;

import java.util.function.Consumer;
import java.util.function.Function;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.util.ReferenceCountUtil;
import io.rsocket.Closeable;
import io.rsocket.transport.ServerTransport;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.WebsocketServerSpec;

import static io.netty.channel.ChannelHandler.Sharable;
import static io.rsocket.frame.FrameLengthCodec.FRAME_LENGTH_MASK;

abstract class BaseWebsocketServerTransport<SELF extends BaseWebsocketServerTransport<SELF, T>, T extends Closeable> implements ServerTransport<T> {

  private static final Logger logger = LoggerFactory.getLogger(BaseWebsocketServerTransport.class);

  private static final ChannelHandler pongHandler = new PongHandler();

  static Function<HttpServer, HttpServer> serverConfigurer =
          server -> server.doOnConnection(connection -> connection.addHandlerLast(pongHandler));

  final WebsocketServerSpec.Builder specBuilder =
          WebsocketServerSpec.builder().maxFramePayloadLength(FRAME_LENGTH_MASK);

  /**
   * Provide a consumer to customize properties of the {@link WebsocketServerSpec} to use for
   * WebSocket upgrades. The consumer is invoked immediately.
   *
   * @param configurer the configurer to apply to the spec
   * @return the same instance for method chaining
   */
  @SuppressWarnings("unchecked")
  public SELF webSocketSpec(Consumer<WebsocketServerSpec.Builder> configurer) {
    configurer.accept(specBuilder);
    return (SELF) this;
  }

  @Override
  public int getMaxFrameLength() {
    return specBuilder.build().maxFramePayloadLength();
  }

  @Sharable
  private static class PongHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
      if (msg instanceof PongWebSocketFrame) {
        logger.debug("received WebSocket Pong Frame");
        ReferenceCountUtil.safeRelease(msg);
        ctx.read();
      }
      else {
        ctx.fireChannelRead(msg);
      }
    }
  }
}
