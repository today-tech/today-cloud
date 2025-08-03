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

package infra.remoting.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.remoting.Channel;
import infra.remoting.Payload;
import infra.remoting.core.ChannelConnector;
import infra.remoting.core.RemotingClient;
import infra.remoting.core.RemotingServer;
import infra.remoting.frame.decoder.PayloadDecoder;
import infra.remoting.transport.netty.client.TcpClientTransport;
import infra.remoting.transport.netty.server.CloseableChannel;
import infra.remoting.transport.netty.server.TcpServerTransport;
import infra.remoting.util.DefaultPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.tcp.TcpClient;
import reactor.netty.tcp.TcpServer;
import reactor.test.StepVerifier;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

public class KeepaliveTests {

  private static final Logger LOG = LoggerFactory.getLogger(KeepaliveTests.class);
  private static final int PORT = 23200;

  private CloseableChannel server;

  @BeforeEach
  void setUp() {
    server = createServer().block();
  }

  @AfterEach
  void tearDown() {
    server.dispose();
    server.onClose().block();
  }

  @Test
  void keepAliveTest() {
    RemotingClient rsocketClient = createClient();

    int expectedCount = 4;
    AtomicBoolean sleepOnce = new AtomicBoolean(true);
    StepVerifier.create(Flux.range(0, expectedCount)
                    .delayElements(Duration.ofMillis(2000))
                    .concatMap(i -> rsocketClient
                            .requestResponse(Mono.just(DefaultPayload.create("")))
                            .doOnNext(__ -> {
                              if (sleepOnce.getAndSet(false)) {
                                try {
                                  LOG.info("Sleeping...");
                                  Thread.sleep(1_000);
                                  LOG.info("Waking up.");
                                }
                                catch (InterruptedException e) {
                                  throw new RuntimeException(e);
                                }
                              }
                            })
                            .log("id " + i)
                            .onErrorComplete()))
            .expectSubscription()
            .expectNextCount(expectedCount)
            .verifyComplete();
  }

  @Test
  void keepAliveTestLazy() {
    Mono<Channel> rsocketMono = createClientLazy();

    int expectedCount = 4;
    AtomicBoolean sleepOnce = new AtomicBoolean(true);
    StepVerifier.create(Flux.range(0, expectedCount)
                    .delayElements(Duration.ofMillis(2000))
                    .concatMap(i -> rsocketMono.flatMap(rsocket -> rsocket
                            .requestResponse(DefaultPayload.create(""))
                            .doOnNext(__ -> {
                              if (sleepOnce.getAndSet(false)) {
                                try {
                                  LOG.info("Sleeping...");
                                  Thread.sleep(1_000);
                                  LOG.info("Waking up.");
                                }
                                catch (InterruptedException e) {
                                  throw new RuntimeException(e);
                                }
                              }
                            })
                            .log("id " + i)
                            .onErrorComplete())))
            .expectSubscription()
            .expectNextCount(expectedCount)
            .verifyComplete();
  }

  private static Mono<CloseableChannel> createServer() {
    LOG.info("Starting server at port {}", PORT);

    TcpServer tcpServer = TcpServer.create().host("localhost").port(PORT);

    return RemotingServer.create((setupPayload, rSocket) -> {
              rSocket
                      .onClose()
                      .doFirst(() -> LOG.info("Connected on server side."))
                      .doOnTerminate(() -> LOG.info("Connection closed on server side."))
                      .subscribe();

              return Mono.just(new MyServerRsocket());
            })
            .payloadDecoder(PayloadDecoder.ZERO_COPY)
            .bind(TcpServerTransport.create(tcpServer))
            .doOnNext(closeableChannel -> LOG.info("RSocket server started."));
  }

  private static RemotingClient createClient() {
    LOG.info("Connecting....");

    Function<String, RetryBackoffSpec> reconnectSpec = reason ->
            Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(10L))
                    .doBeforeRetry(retrySignal -> LOG.info("Reconnecting. Reason: {}", reason));

    Mono<Channel> rsocketMono =
            ChannelConnector.create()
                    .fragment(16384)
                    .reconnect(reconnectSpec.apply("connector-close"))
                    .keepAlive(Duration.ofMillis(100L), Duration.ofMillis(900L))
                    .connect(TcpClientTransport.create(TcpClient.create().host("localhost").port(PORT)));

    RemotingClient client = RemotingClient.from(rsocketMono);

    client.source()
            .doOnNext(r -> LOG.info("Got"))
            .flatMap(Channel::onClose)
            .doOnError(err -> LOG.error("Error during onClose.", err))
            .retryWhen(reconnectSpec.apply("client-close"))
            .doFirst(() -> LOG.info("Connected on client side."))
            .doOnTerminate(() -> LOG.info("Connection closed on client side."))
            .repeat()
            .subscribe();

    return client;
  }

  private static Mono<Channel> createClientLazy() {
    LOG.info("Connecting....");

    Function<String, RetryBackoffSpec> reconnectSpec =
            reason ->
                    Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(10L))
                            .doBeforeRetry(retrySignal -> LOG.info("Reconnecting. Reason: {}", reason));

    return ChannelConnector.create()
            .fragment(16384)
            .reconnect(reconnectSpec.apply("connector-close"))
            .keepAlive(Duration.ofMillis(100L), Duration.ofMillis(900L))
            .connect(TcpClientTransport.create(TcpClient.create().host("localhost").port(PORT)));
  }

  public static class MyServerRsocket implements Channel {

    @Override
    public Mono<Payload> requestResponse(Payload payload) {
      return Mono.just("Pong").map(DefaultPayload::create);
    }
  }
}
