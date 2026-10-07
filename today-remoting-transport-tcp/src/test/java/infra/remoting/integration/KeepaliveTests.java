/*
 * Copyright 2021 - 2026 the TODAY authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    RemotingClient client = createClient();

    int expectedCount = 4;
    AtomicBoolean sleepOnce = new AtomicBoolean(true);
    StepVerifier.create(Flux.range(0, expectedCount)
                    .delayElements(Duration.ofMillis(2000))
                    .concatMap(i -> client
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
    Mono<Channel> channelMono = createClientLazy();

    int expectedCount = 4;
    AtomicBoolean sleepOnce = new AtomicBoolean(true);
    StepVerifier.create(Flux.range(0, expectedCount)
                    .delayElements(Duration.ofMillis(2000))
                    .concatMap(i -> channelMono.flatMap(channel -> channel
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

    return RemotingServer.create((setupPayload, channel) -> {
              channel
                      .onClose()
                      .doFirst(() -> LOG.info("Connected on server side."))
                      .doOnTerminate(() -> LOG.info("Connection closed on server side."))
                      .subscribe();

              return Mono.just(new MyServerChannel());
            })
            .payloadDecoder(PayloadDecoder.ZERO_COPY)
            .bind(TcpServerTransport.create(tcpServer))
            .doOnNext(closeableChannel -> LOG.info("server started."));
  }

  private static RemotingClient createClient() {
    LOG.info("Connecting....");

    Function<String, RetryBackoffSpec> reconnectSpec = reason ->
            Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(10L))
                    .doBeforeRetry(retrySignal -> LOG.info("Reconnecting. Reason: {}", reason));

    Mono<Channel> channelMono =
            ChannelConnector.create()
                    .fragment(16384)
                    .reconnect(reconnectSpec.apply("connector-close"))
                    .keepAlive(Duration.ofMillis(100L), Duration.ofMillis(900L))
                    .connect(TcpClientTransport.create(TcpClient.create().host("localhost").port(PORT)));

    RemotingClient client = RemotingClient.from(channelMono);

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

  public static class MyServerChannel implements Channel {

    @Override
    public Mono<Payload> requestResponse(Payload payload) {
      return Mono.just("Pong").map(DefaultPayload::create);
    }
  }
}
