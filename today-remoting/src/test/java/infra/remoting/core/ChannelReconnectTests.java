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
package infra.remoting.core;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import infra.remoting.FrameAssert;
import infra.remoting.Channel;
import infra.remoting.frame.FrameType;
import infra.remoting.test.util.TestClientTransport;
import infra.remoting.test.util.TestDuplexConnection;
import infra.remoting.transport.ClientTransport;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import static org.assertj.core.api.Assertions.assertThat;

public class ChannelReconnectTests {

  private Queue<Retry.RetrySignal> retries = new ConcurrentLinkedQueue<>();

  @Test
  public void shouldBeASharedReconnectableInstanceOfChannelMono() throws InterruptedException {
    TestClientTransport[] testClientTransport =
            new TestClientTransport[] { new TestClientTransport() };
    Mono<Channel> channelMono =
            ChannelConnector.create()
                    .reconnect(Retry.indefinitely())
                    .connect(() -> testClientTransport[0]);

    Channel channel1 = channelMono.block();
    Channel channel2 = channelMono.block();

    FrameAssert.assertThat(testClientTransport[0].testConnection().awaitFrame())
            .typeOf(FrameType.SETUP)
            .hasStreamIdZero()
            .hasNoLeaks();

    assertThat(channel1).isEqualTo(channel2);

    testClientTransport[0].testConnection().dispose();
    channel1.onClose().block(Duration.ofSeconds(1));
    testClientTransport[0].alloc().assertHasNoLeaks();
    testClientTransport[0] = new TestClientTransport();

    Channel channel3 = channelMono.block();
    Channel channel4 = channelMono.block();

    FrameAssert.assertThat(testClientTransport[0].testConnection().awaitFrame())
            .typeOf(FrameType.SETUP)
            .hasStreamIdZero()
            .hasNoLeaks();

    assertThat(channel3).isEqualTo(channel4).isNotEqualTo(channel2);

    testClientTransport[0].testConnection().dispose();
    channel3.onClose().block(Duration.ofSeconds(1));
    testClientTransport[0].alloc().assertHasNoLeaks();
  }

  @Test
  @SuppressWarnings({ "rawtype" })
  public void shouldBeRetrieableConnectionSharedReconnectableInstanceOfChannelMono() {
    ClientTransport transport = Mockito.mock(ClientTransport.class);
    TestClientTransport transport1 = new TestClientTransport();
    Mockito.when(transport.connect())
            .thenThrow(UncheckedIOException.class)
            .thenThrow(UncheckedIOException.class)
            .thenThrow(UncheckedIOException.class)
            .thenThrow(UncheckedIOException.class)
            .thenReturn(transport1.connect());
    Mono<Channel> channelMono =
            ChannelConnector.create()
                    .reconnect(
                            Retry.backoff(4, Duration.ofMillis(100))
                                    .maxBackoff(Duration.ofMillis(500))
                                    .doAfterRetry(onRetry()))
                    .connect(transport);

    Channel channel1 = channelMono.block();
    Channel channel2 = channelMono.block();

    assertThat(channel1).isEqualTo(channel2);
    assertRetries(
            UncheckedIOException.class,
            UncheckedIOException.class,
            UncheckedIOException.class,
            UncheckedIOException.class);

    FrameAssert.assertThat(transport1.testConnection().awaitFrame())
            .typeOf(FrameType.SETUP)
            .hasStreamIdZero()
            .hasNoLeaks();

    transport1.testConnection().dispose();
    channel1.onClose().block(Duration.ofSeconds(1));
    transport1.alloc().assertHasNoLeaks();
  }

  @Test
  @SuppressWarnings({ "rawtype" })
  public void shouldBeExaustedRetrieableConnectionSharedReconnectableInstanceOfChannelMono() {
    ClientTransport transport = Mockito.mock(ClientTransport.class);
    TestClientTransport transport1 = new TestClientTransport();
    Mockito.when(transport.connect())
            .thenThrow(UncheckedIOException.class)
            .thenThrow(UncheckedIOException.class)
            .thenThrow(UncheckedIOException.class)
            .thenThrow(UncheckedIOException.class)
            .thenThrow(UncheckedIOException.class)
            .thenReturn(transport1.connect());
    Mono<Channel> channelMono =
            ChannelConnector.create()
                    .reconnect(
                            Retry.backoff(4, Duration.ofMillis(100))
                                    .maxBackoff(Duration.ofMillis(500))
                                    .doAfterRetry(onRetry()))
                    .connect(transport);

    Assertions.assertThatThrownBy(channelMono::block)
            .matches(Exceptions::isRetryExhausted)
            .hasCauseInstanceOf(UncheckedIOException.class);

    Assertions.assertThatThrownBy(channelMono::block)
            .matches(Exceptions::isRetryExhausted)
            .hasCauseInstanceOf(UncheckedIOException.class);

    assertRetries(
            UncheckedIOException.class,
            UncheckedIOException.class,
            UncheckedIOException.class,
            UncheckedIOException.class);

    transport1.alloc().assertHasNoLeaks();
  }

  @Test
  public void shouldBeNotBeASharedReconnectableInstanceOfChannelMono() {
    TestClientTransport transport = new TestClientTransport();
    Mono<Channel> channelMono = ChannelConnector.connectWith(transport);

    Channel channel1 = channelMono.block();
    TestDuplexConnection connection1 = transport.testConnection();

    FrameAssert.assertThat(connection1.awaitFrame())
            .typeOf(FrameType.SETUP)
            .hasStreamIdZero()
            .hasNoLeaks();

    Channel channel2 = channelMono.block();
    TestDuplexConnection connection2 = transport.testConnection();

    assertThat(channel1).isNotEqualTo(channel2);

    FrameAssert.assertThat(connection2.awaitFrame())
            .typeOf(FrameType.SETUP)
            .hasStreamIdZero()
            .hasNoLeaks();

    connection1.dispose();
    connection2.dispose();
    channel1.onClose().block(Duration.ofSeconds(1));
    channel2.onClose().block(Duration.ofSeconds(1));
    transport.alloc().assertHasNoLeaks();
  }

  @SafeVarargs
  private final void assertRetries(Class<? extends Throwable>... exceptions) {
    assertThat(retries.size()).isEqualTo(exceptions.length);
    int index = 0;
    for (Iterator<Retry.RetrySignal> it = retries.iterator(); it.hasNext(); ) {
      Retry.RetrySignal retryContext = it.next();
      assertThat(retryContext.totalRetries()).isEqualTo(index);
      assertThat(retryContext.failure().getClass()).isEqualTo(exceptions[index]);
      index++;
    }
  }

  Consumer<Retry.RetrySignal> onRetry() {
    return context -> retries.add(context);
  }
}
