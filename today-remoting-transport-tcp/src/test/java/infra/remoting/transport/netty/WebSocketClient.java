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

package infra.remoting.transport.netty;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

/**
 * This is an example of a WebSocket client.
 *
 * <p>In order to run this example you need a compatible WebSocket server. Therefore you can either
 * start the WebSocket server from the examples or connect to an existing WebSocket server such as
 * <a href="http://www.websocket.org/echo.html">ws://echo.websocket.org</a>.
 *
 * <p>The client will attempt to connect to the URI passed to it as the first argument. You don't
 * have to specify any arguments if you want to connect to the example WebSocket server, as this is
 * the default.
 */
public final class WebSocketClient {

  static final String URL = System.getProperty("url", "ws://127.0.0.1:7878/websocket");

  public static void main(String[] args) throws Exception {
    URI uri = new URI(URL);
    String scheme = uri.getScheme() == null ? "ws" : uri.getScheme();
    final String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
    final int port;
    if (uri.getPort() == -1) {
      if ("ws".equalsIgnoreCase(scheme)) {
        port = 80;
      }
      else if ("wss".equalsIgnoreCase(scheme)) {
        port = 443;
      }
      else {
        port = -1;
      }
    }
    else {
      port = uri.getPort();
    }

    if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
      System.err.println("Only WS(S) is supported.");
      return;
    }

    final boolean ssl = "wss".equalsIgnoreCase(scheme);
    final SslContext sslCtx;
    if (ssl) {
      sslCtx =
              SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
    }
    else {
      sslCtx = null;
    }

    EventLoopGroup group = new NioEventLoopGroup();
    try {
      // Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
      // If you change it to V00, ping is not supported and remember to change
      // HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
      final WebSocketClientHandler handler =
              new WebSocketClientHandler(
                      WebSocketClientHandshakerFactory.newHandshaker(
                              uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders()));

      Bootstrap b = new Bootstrap();
      b.group(group)
              .channel(NioSocketChannel.class)
              .handler(
                      new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                          ChannelPipeline p = ch.pipeline();
                          if (sslCtx != null) {
                            p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                          }
                          p.addLast(
                                  new HttpClientCodec(),
                                  new HttpObjectAggregator(8192),
                                  WebSocketClientCompressionHandler.INSTANCE,
                                  handler);
                        }
                      });

      Channel ch = b.connect(uri.getHost(), port).sync().channel();
      handler.handshakeFuture().sync();

      BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
      while (true) {
        String msg = console.readLine();
        if (msg == null) {
          break;
        }
        else if ("bye".equals(msg.toLowerCase())) {
          ch.writeAndFlush(new CloseWebSocketFrame());
          ch.closeFuture().sync();
          break;
        }
        else if ("ping".equals(msg.toLowerCase())) {
          WebSocketFrame frame =
                  new PingWebSocketFrame(Unpooled.wrappedBuffer(new byte[] { 8, 1, 8, 1 }));
          ch.writeAndFlush(frame);
        }
        else if ("pong".equals(msg.toLowerCase())) {
          WebSocketFrame frame =
                  new PongWebSocketFrame(Unpooled.wrappedBuffer(new byte[] { 8, 1, 8, 1 }));
          ch.writeAndFlush(frame);
        }
        else {
          WebSocketFrame frame = new TextWebSocketFrame(msg);
          ch.writeAndFlush(frame);
        }
      }
    }
    finally {
      group.shutdownGracefully();
    }
  }
}
