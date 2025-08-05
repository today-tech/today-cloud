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

package infra.remoting.resume;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import infra.remoting.Channel;
import infra.remoting.ChannelAcceptor;
import infra.remoting.Payload;
import infra.remoting.core.ChannelConnector;
import infra.remoting.core.RemotingServer;
import infra.remoting.core.Resume;
import infra.remoting.error.RejectedResumeException;
import infra.remoting.error.UnsupportedSetupException;
import infra.remoting.test.SlowTest;
import infra.remoting.transport.ClientTransport;
import infra.remoting.transport.ServerTransport;
import infra.remoting.transport.netty.client.TcpClientTransport;
import infra.remoting.transport.netty.server.CloseableChannel;
import infra.remoting.transport.netty.server.TcpServerTransport;
import infra.remoting.util.DefaultPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.retry.Retry;

@SlowTest
public class ResumeIntegrationTests {

  private static final String SERVER_HOST = "localhost";

  private static final int SERVER_PORT = 0;

  @Test
  void timeoutOnPermanentDisconnect() {
    CloseableChannel closeable = newServerChannel().block();

    DisconnectableClientTransport clientTransport =
            new DisconnectableClientTransport(clientTransport(closeable.address()));

    int sessionDurationSeconds = 5;
    Channel channel = newClientChannel(clientTransport, sessionDurationSeconds).block();

    Mono.delay(Duration.ofSeconds(1)).subscribe(v -> clientTransport.disconnectPermanently());

    StepVerifier.create(channel.requestChannel(testRequest()).then().doFinally(s -> closeable.dispose()))
            .expectError(ClosedChannelException.class)
            .verify(Duration.ofSeconds(7));
  }

  @Test
  public void reconnectOnDisconnect() {
    CloseableChannel closeable = newServerChannel().block();

    DisconnectableClientTransport clientTransport =
            new DisconnectableClientTransport(clientTransport(closeable.address()));

    int sessionDurationSeconds = 15;
    Channel channel = newClientChannel(clientTransport, sessionDurationSeconds).block();

    Flux.just(3, 20, 40, 75)
            .flatMap(v -> Mono.delay(Duration.ofSeconds(v)))
            .subscribe(v -> clientTransport.disconnectFor(Duration.ofSeconds(7)));

    AtomicInteger counter = new AtomicInteger(-1);
    StepVerifier.create(
                    channel
                            .requestChannel(testRequest())
                            .take(Duration.ofSeconds(600))
                            .map(Payload::getDataUtf8)
                            .timeout(Duration.ofSeconds(12))
                            .doOnNext(x -> throwOnNonContinuous(counter, x))
                            .then()
                            .doFinally(s -> closeable.dispose()))
            .expectComplete()
            .verify();
  }

  @Test
  public void reconnectOnMissingSession() {

    int serverSessionDuration = 2;

    CloseableChannel closeable = newServerChannel(serverSessionDuration).block();

    DisconnectableClientTransport clientTransport =
            new DisconnectableClientTransport(clientTransport(closeable.address()));
    int clientSessionDurationSeconds = 10;

    Channel channel = newClientChannel(clientTransport, clientSessionDurationSeconds).block();

    Mono.delay(Duration.ofSeconds(1))
            .subscribe(v -> clientTransport.disconnectFor(Duration.ofSeconds(3)));

    StepVerifier.create(
                    channel.requestChannel(testRequest()).then().doFinally(s -> closeable.dispose()))
            .expectError()
            .verify(Duration.ofSeconds(5));

    StepVerifier.create(channel.onClose())
            .expectErrorMatches(
                    err ->
                            err instanceof RejectedResumeException
                                    && "unknown resume token".equals(err.getMessage()))
            .verify(Duration.ofSeconds(5));
  }

  @Test
  void serverMissingResume() {
    CloseableChannel closeableChannel =
            RemotingServer.create(ChannelAcceptor.with(new TestResponderChannel()))
                    .bind(serverTransport(SERVER_HOST, SERVER_PORT))
                    .block();

    Channel channel = ChannelConnector.create()
            .resume(new Resume())
            .connect(clientTransport(closeableChannel.address()))
            .block();

    StepVerifier.create(channel.onClose().doFinally(s -> closeableChannel.dispose()))
            .expectErrorMatches(err -> err instanceof UnsupportedSetupException
                    && "resume not supported".equals(err.getMessage()))
            .verify(Duration.ofSeconds(5));

    Assertions.assertThat(channel.isDisposed()).isTrue();
  }

  static ClientTransport clientTransport(InetSocketAddress address) {
    return TcpClientTransport.create(address);
  }

  static ServerTransport<CloseableChannel> serverTransport(String host, int port) {
    return TcpServerTransport.create(host, port);
  }

  private static Flux<Payload> testRequest() {
    return Flux.interval(Duration.ofMillis(500))
            .map(v -> DefaultPayload.create("client_request"))
            .onBackpressureDrop();
  }

  private void throwOnNonContinuous(AtomicInteger counter, String x) {
    int curValue = Integer.parseInt(x);
    int prevValue = counter.get();
    if (prevValue >= 0) {
      int dif = curValue - prevValue;
      if (dif != 1) {
        throw new IllegalStateException(String.format(
                "Payload values are expected to be continuous numbers: %d %d", prevValue, curValue));
      }
    }
    counter.set(curValue);
  }

  private static Mono<Channel> newClientChannel(
          DisconnectableClientTransport clientTransport, int sessionDurationSeconds) {
    return ChannelConnector.create()
            .resume(new Resume()
                    .sessionDuration(Duration.ofSeconds(sessionDurationSeconds))
                    .storeFactory(t -> new InMemoryResumableFramesStore("client", t, 500_000))
                    .cleanupStoreOnKeepAlive()
                    .retry(Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(1))))
            .keepAlive(Duration.ofSeconds(5), Duration.ofMinutes(5))
            .connect(clientTransport);
  }

  private static Mono<CloseableChannel> newServerChannel() {
    return newServerChannel(15);
  }

  private static Mono<CloseableChannel> newServerChannel(int sessionDurationSeconds) {
    return RemotingServer.create(ChannelAcceptor.with(new TestResponderChannel()))
            .resume(
                    new Resume()
                            .sessionDuration(Duration.ofSeconds(sessionDurationSeconds))
                            .cleanupStoreOnKeepAlive()
                            .storeFactory(t -> new InMemoryResumableFramesStore("server", t, 500_000)))
            .bind(serverTransport(SERVER_HOST, SERVER_PORT));
  }

  private static class TestResponderChannel implements Channel {

    AtomicInteger counter = new AtomicInteger();

    @Override
    public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
      return duplicate(
              Flux.interval(Duration.ofMillis(1))
                      .onBackpressureLatest()
                      .publishOn(Schedulers.boundedElastic()),
              20)
              .map(v -> DefaultPayload.create(String.valueOf(counter.getAndIncrement())))
              .takeUntilOther(Flux.from(payloads).then());
    }

    private <T> Flux<T> duplicate(Flux<T> f, int n) {
      Flux<T> r = Flux.empty();
      for (int i = 0; i < n; i++) {
        r = r.mergeWith(f);
      }
      return r;
    }
  }
}
