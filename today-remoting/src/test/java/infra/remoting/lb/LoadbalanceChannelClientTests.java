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

package infra.remoting.lb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import infra.remoting.Channel;
import infra.remoting.Payload;
import infra.remoting.core.ChannelConnector;
import infra.remoting.core.RemotingClient;
import infra.remoting.transport.ClientTransport;
import infra.remoting.util.DefaultPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoadbalanceChannelClientTests {

  @Mock
  private ClientTransport clientTransport;
  @Mock
  private ChannelConnector channelConnector;

  public static final Duration SHORT_DURATION = Duration.ofMillis(25);
  public static final Duration LONG_DURATION = Duration.ofMillis(75);

  private static final Publisher<Payload> SOURCE =
          Flux.interval(SHORT_DURATION)
                  .onBackpressureBuffer()
                  .map(String::valueOf)
                  .map(DefaultPayload::create);

  private static final Mono<Channel> PROGRESSING_HANDLER =
          Mono.just(
                  new Channel() {
                    private final AtomicInteger i = new AtomicInteger();

                    @Override
                    public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
                      return Flux.from(payloads)
                              .delayElements(SHORT_DURATION)
                              .map(Payload::getDataUtf8)
                              .map(DefaultPayload::create)
                              .take(i.incrementAndGet());
                    }
                  });

  @Test
  void testChannelReconnection() {
    when(channelConnector.connect(clientTransport)).thenReturn(PROGRESSING_HANDLER);

    RemotingClient client = RemotingClient.forLoadBalance(channelConnector,
            Mono.just(singletonList(LoadBalanceTarget.of("key", clientTransport))));

    Publisher<String> result = client
            .requestChannel(SOURCE)
            .repeatWhen(longFlux -> longFlux.delayElements(LONG_DURATION).take(5))
            .map(Payload::getDataUtf8)
            .log();

    StepVerifier.create(result)
            .expectSubscription()
            .assertNext(s -> assertThat(s).isEqualTo("0"))
            .assertNext(s -> assertThat(s).isEqualTo("0"))
            .assertNext(s -> assertThat(s).isEqualTo("1"))
            .assertNext(s -> assertThat(s).isEqualTo("0"))
            .assertNext(s -> assertThat(s).isEqualTo("1"))
            .assertNext(s -> assertThat(s).isEqualTo("2"))
            .assertNext(s -> assertThat(s).isEqualTo("0"))
            .assertNext(s -> assertThat(s).isEqualTo("1"))
            .assertNext(s -> assertThat(s).isEqualTo("2"))
            .assertNext(s -> assertThat(s).isEqualTo("3"))
            .assertNext(s -> assertThat(s).isEqualTo("0"))
            .assertNext(s -> assertThat(s).isEqualTo("1"))
            .assertNext(s -> assertThat(s).isEqualTo("2"))
            .assertNext(s -> assertThat(s).isEqualTo("3"))
            .assertNext(s -> assertThat(s).isEqualTo("4"))
            .verifyComplete();

    verify(channelConnector).connect(clientTransport);
    verifyNoMoreInteractions(channelConnector, clientTransport);
  }
}
