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

package infra.remoting.micrometer.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import infra.remoting.Channel;
import infra.remoting.Payload;
import infra.remoting.core.ChannelConnector;
import infra.remoting.core.RemotingServer;
import infra.remoting.plugins.ChannelAcceptorInterceptor;
import infra.remoting.plugins.ChannelDecorator;
import infra.remoting.plugins.ConnectionDecorator;
import infra.remoting.test.TestSubscriber;
import infra.remoting.transport.netty.client.TcpClientTransport;
import infra.remoting.transport.netty.server.CloseableChannel;
import infra.remoting.transport.netty.server.TcpServerTransport;
import infra.remoting.util.ChannelWrapper;
import infra.remoting.util.DefaultPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class IntegrationTests {

  private static final ChannelDecorator requesterInterceptor;
  private static final ChannelDecorator responderInterceptor;
  private static final ChannelAcceptorInterceptor clientAcceptorInterceptor;
  private static final ChannelAcceptorInterceptor serverAcceptorInterceptor;
  private static final ConnectionDecorator CONNECTION_DECORATOR;

  private static volatile boolean calledRequester = false;
  private static volatile boolean calledResponder = false;
  private static volatile boolean calledClientAcceptor = false;
  private static volatile boolean calledServerAcceptor = false;
  private static volatile boolean calledFrame = false;

  static {
    requesterInterceptor = channel ->
            new ChannelWrapper(channel) {
              @Override
              public Mono<Payload> requestResponse(Payload payload) {
                calledRequester = true;
                return channel.requestResponse(payload);
              }
            };

    responderInterceptor = channel ->
            new ChannelWrapper(channel) {
              @Override
              public Mono<Payload> requestResponse(Payload payload) {
                calledResponder = true;
                return channel.requestResponse(payload);
              }
            };

    clientAcceptorInterceptor = acceptor ->
            (setup, channel) -> {
              calledClientAcceptor = true;
              return acceptor.accept(setup, channel);
            };

    serverAcceptorInterceptor = acceptor -> (setup, channel) -> {
      calledServerAcceptor = true;
      return acceptor.accept(setup, channel);
    };

    CONNECTION_DECORATOR = (type, connection) -> {
      calledFrame = true;
      return connection;
    };
  }

  private CloseableChannel server;
  private Channel client;
  private AtomicInteger requestCount;
  private CountDownLatch disconnectionCounter;
  private AtomicInteger errorCount;

  @BeforeEach
  public void startup() {
    errorCount = new AtomicInteger();
    requestCount = new AtomicInteger();
    disconnectionCounter = new CountDownLatch(1);

    server = RemotingServer.create((setup, channel) -> {
              channel.onClose()
                      .doFinally(signalType -> disconnectionCounter.countDown())
                      .subscribe();

              return Mono.just(new Channel() {
                @Override
                public Mono<Payload> requestResponse(Payload payload) {
                  return Mono.just(DefaultPayload.create("RESPONSE", "METADATA"))
                          .doOnSubscribe(s -> requestCount.incrementAndGet());
                }

                @Override
                public Flux<Payload> requestStream(Payload payload) {
                  return Flux.range(1, 10_000)
                          .map(i -> DefaultPayload.create("data -> " + i));
                }

                @Override
                public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
                  return Flux.from(payloads);
                }
              });
            })
            .interceptors(registry -> registry
                    .forResponder(responderInterceptor)
                    .forChannelAcceptor(serverAcceptorInterceptor)
                    .forConnection(CONNECTION_DECORATOR))
            .bind(TcpServerTransport.create("localhost", 0))
            .block();

    client = ChannelConnector.create()
            .interceptors(registry -> registry
                    .forRequester(requesterInterceptor)
                    .forChannelAcceptor(clientAcceptorInterceptor)
                    .forConnection(CONNECTION_DECORATOR))
            .connect(TcpClientTransport.create(server.address()))
            .block();
  }

  @AfterEach
  public void teardown() {
    server.dispose();
  }

  @Test
  @Timeout(5_000L)
  public void testRequest() {
    client.requestResponse(DefaultPayload.create("REQUEST", "META")).block();
    assertThat(requestCount).as("Server did not see the request.").hasValue(1);

    assertThat(calledRequester).isTrue();
    assertThat(calledResponder).isTrue();
    assertThat(calledClientAcceptor).isTrue();
    assertThat(calledServerAcceptor).isTrue();
    assertThat(calledFrame).isTrue();
  }

  @Test
  @Timeout(5_000L)
  public void testStream() {
    Subscriber<Payload> subscriber = TestSubscriber.createCancelling();
    client.requestStream(DefaultPayload.create("start")).subscribe(subscriber);

    verify(subscriber).onSubscribe(any());
    verifyNoMoreInteractions(subscriber);
  }

  @Test
  @Timeout(5_000L)
  public void testClose() throws InterruptedException {
    client.dispose();
    disconnectionCounter.await();
  }

  @Test // (timeout = 5_000L)
  public void testCallRequestWithErrorAndThenRequest() {
    assertThatThrownBy(client.requestChannel(Mono.error(new Throwable("test")))::blockLast)
            .hasMessage("java.lang.Throwable: test");

    testRequest();
  }
}
