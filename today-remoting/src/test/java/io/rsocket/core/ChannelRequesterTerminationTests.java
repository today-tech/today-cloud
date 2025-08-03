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

package io.rsocket.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Publisher;

import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.Arrays;
import java.util.function.Function;

import io.rsocket.FrameAssert;
import io.rsocket.Payload;
import io.rsocket.Channel;
import io.rsocket.core.ChannelRequesterTests.ClientSocketRule;
import io.rsocket.frame.FrameType;
import io.rsocket.util.EmptyPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class ChannelRequesterTerminationTests {

  public final ClientSocketRule rule = new ClientSocketRule();

  @BeforeEach
  public void setup() {
    rule.init();
  }

  @AfterEach
  public void tearDownAndCheckNoLeaks() {
    rule.assertHasNoLeaks();
  }

  @ParameterizedTest
  @MethodSource("rsocketInteractions")
  public void testCurrentStreamIsTerminatedOnConnectionClose(
          FrameType requestType, Function<Channel, ? extends Publisher<?>> interaction) {
    ChannelRequester rSocket = rule.socket;

    StepVerifier.create(interaction.apply(rSocket))
            .then(
                    () -> {
                      FrameAssert.assertThat(rule.connection.pollFrame()).typeOf(requestType).hasNoLeaks();
                    })
            .then(() -> rule.connection.dispose())
            .expectError(ClosedChannelException.class)
            .verify(Duration.ofSeconds(5));
  }

  @ParameterizedTest
  @MethodSource("rsocketInteractions")
  public void testSubsequentStreamIsTerminatedAfterConnectionClose(
          FrameType requestType, Function<Channel, ? extends Publisher<?>> interaction) {
    ChannelRequester rSocket = rule.socket;

    rule.connection.dispose();
    StepVerifier.create(interaction.apply(rSocket))
            .expectError(ClosedChannelException.class)
            .verify(Duration.ofSeconds(5));
  }

  public static Iterable<Arguments> rsocketInteractions() {
    EmptyPayload payload = EmptyPayload.INSTANCE;

    Arguments resp =
            Arguments.of(
                    FrameType.REQUEST_RESPONSE,
                    new Function<Channel, Mono<Payload>>() {
                      @Override
                      public Mono<Payload> apply(Channel rSocket) {
                        return rSocket.requestResponse(payload);
                      }

                      @Override
                      public String toString() {
                        return "Request Response";
                      }
                    });
    Arguments stream =
            Arguments.of(
                    FrameType.REQUEST_STREAM,
                    new Function<Channel, Flux<Payload>>() {
                      @Override
                      public Flux<Payload> apply(Channel rSocket) {
                        return rSocket.requestStream(payload);
                      }

                      @Override
                      public String toString() {
                        return "Request Stream";
                      }
                    });
    Arguments channel =
            Arguments.of(
                    FrameType.REQUEST_CHANNEL,
                    new Function<Channel, Flux<Payload>>() {
                      @Override
                      public Flux<Payload> apply(Channel rSocket) {
                        return rSocket.requestChannel(Flux.<Payload>never().startWith(payload));
                      }

                      @Override
                      public String toString() {
                        return "Request Channel";
                      }
                    });

    return Arrays.asList(resp, stream, channel);
  }
}
