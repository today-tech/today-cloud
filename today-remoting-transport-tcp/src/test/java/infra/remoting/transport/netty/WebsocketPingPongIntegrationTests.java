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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.stream.Stream;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.ReferenceCountUtil;
import io.rsocket.Closeable;
import io.rsocket.Channel;
import io.rsocket.ChannelAcceptor;
import io.rsocket.core.ChannelConnector;
import io.rsocket.core.RemotingServer;
import io.rsocket.transport.ServerTransport;
import infra.remoting.transport.netty.client.WebsocketClientTransport;
import infra.remoting.transport.netty.server.WebsocketRouteTransport;
import infra.remoting.transport.netty.server.WebsocketServerTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.Scannable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServer;
import reactor.test.StepVerifier;

public class WebsocketPingPongIntegrationTests {
  private static final String host = "localhost";
  private static final int port = 8088;

  private Closeable server;

  @AfterEach
  void tearDown() {
    server.dispose();
  }

  @ParameterizedTest
  @MethodSource("provideServerTransport")
  void webSocketPingPong(ServerTransport<Closeable> serverTransport) {
    server =
            RemotingServer.create(ChannelAcceptor.forRequestResponse(Mono::just))
                    .bind(serverTransport)
                    .block();

    String expectedData = "data";
    String expectedPing = "ping";

    PingSender pingSender = new PingSender();

    HttpClient httpClient =
            HttpClient.create()
                    .tcpConfiguration(
                            tcpClient ->
                                    tcpClient
                                            .doOnConnected(b -> b.addHandlerLast(pingSender))
                                            .host(host)
                                            .port(port));

    Channel channel =
            ChannelConnector.connectWith(WebsocketClientTransport.create(httpClient, "/")).block();

    channel
            .requestResponse(DefaultPayload.create(expectedData))
            .delaySubscription(pingSender.sendPing(expectedPing))
            .as(StepVerifier::create)
            .expectNextMatches(p -> expectedData.equals(p.getDataUtf8()))
            .expectComplete()
            .verify(Duration.ofSeconds(5));

    pingSender
            .receivePong()
            .as(StepVerifier::create)
            .expectNextMatches(expectedPing::equals)
            .expectComplete()
            .verify(Duration.ofSeconds(5));

    channel
            .requestResponse(DefaultPayload.create(expectedData))
            .delaySubscription(pingSender.sendPong())
            .as(StepVerifier::create)
            .expectNextMatches(p -> expectedData.equals(p.getDataUtf8()))
            .expectComplete()
            .verify(Duration.ofSeconds(5));
  }

  private static Stream<Arguments> provideServerTransport() {
    return Stream.of(
            Arguments.of(WebsocketServerTransport.create(host, port)),
            Arguments.of(
                    new WebsocketRouteTransport(
                            HttpServer.create().host(host).port(port), routes -> { }, "/")));
  }

  private static class PingSender extends ChannelInboundHandlerAdapter {
    private final Sinks.One<io.netty.channel.Channel> channel = Sinks.one();
    private final Sinks.One<String> pong = Sinks.one();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      if (msg instanceof PongWebSocketFrame) {
        pong.tryEmitValue(((PongWebSocketFrame) msg).content().toString(StandardCharsets.UTF_8));
        ReferenceCountUtil.safeRelease(msg);
        ctx.read();
      }
      else {
        super.channelRead(ctx, msg);
      }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
      io.netty.channel.Channel ch = ctx.channel();
      if (!(channel.scan(Scannable.Attr.TERMINATED)) && ch.isWritable()) {
        channel.tryEmitValue(ctx.channel());
      }
      super.channelWritabilityChanged(ctx);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
      io.netty.channel.Channel ch = ctx.channel();
      if (ch.isWritable()) {
        channel.tryEmitValue(ch);
      }
      super.handlerAdded(ctx);
    }

    public Mono<Void> sendPing(String data) {
      return send(
              new PingWebSocketFrame(Unpooled.wrappedBuffer(data.getBytes(StandardCharsets.UTF_8))));
    }

    public Mono<Void> sendPong() {
      return send(new PongWebSocketFrame());
    }

    public Mono<String> receivePong() {
      return pong.asMono();
    }

    private Mono<Void> send(WebSocketFrame webSocketFrame) {
      return channel.asMono().doOnNext(ch -> ch.writeAndFlush(webSocketFrame)).then();
    }
  }
}
