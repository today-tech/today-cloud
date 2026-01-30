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

package infra.remoting.core;

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

import infra.remoting.Channel;
import infra.remoting.FrameAssert;
import infra.remoting.Payload;
import infra.remoting.core.RequesterChannelTests.ClientChannelRule;
import infra.remoting.frame.FrameType;
import infra.remoting.util.EmptyPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class RequesterChannelTerminationTests {

  public final ClientChannelRule rule = new ClientChannelRule();

  @BeforeEach
  public void setup() {
    rule.init();
  }

  @AfterEach
  public void tearDownAndCheckNoLeaks() {
    rule.assertHasNoLeaks();
  }

  @ParameterizedTest
  @MethodSource("interactions")
  public void testCurrentStreamIsTerminatedOnConnectionClose(
          FrameType requestType, Function<Channel, ? extends Publisher<?>> interaction) {
    RequesterChannel channel = rule.channel;

    StepVerifier.create(interaction.apply(channel))
            .then(
                    () -> {
                      FrameAssert.assertThat(rule.connection.pollFrame()).typeOf(requestType).hasNoLeaks();
                    })
            .then(() -> rule.connection.dispose())
            .expectError(ClosedChannelException.class)
            .verify(Duration.ofSeconds(5));
  }

  @ParameterizedTest
  @MethodSource("interactions")
  public void testSubsequentStreamIsTerminatedAfterConnectionClose(
          FrameType requestType, Function<Channel, ? extends Publisher<?>> interaction) {
    RequesterChannel channel = rule.channel;

    rule.connection.dispose();
    StepVerifier.create(interaction.apply(channel))
            .expectError(ClosedChannelException.class)
            .verify(Duration.ofSeconds(5));
  }

  public static Iterable<Arguments> interactions() {
    EmptyPayload payload = EmptyPayload.INSTANCE;

    Arguments resp =
            Arguments.of(
                    FrameType.REQUEST_RESPONSE,
                    new Function<Channel, Mono<Payload>>() {
                      @Override
                      public Mono<Payload> apply(Channel channel) {
                        return channel.requestResponse(payload);
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
                      public Flux<Payload> apply(Channel channel) {
                        return channel.requestStream(payload);
                      }

                      @Override
                      public String toString() {
                        return "Request Stream";
                      }
                    });
    Arguments channel =
            Arguments.of(FrameType.REQUEST_CHANNEL,
                    new Function<Channel, Flux<Payload>>() {
                      @Override
                      public Flux<Payload> apply(Channel channel) {
                        return channel.requestChannel(Flux.<Payload>never().startWith(payload));
                      }

                      @Override
                      public String toString() {
                        return "Request Channel";
                      }
                    });

    return Arrays.asList(resp, stream, channel);
  }
}
