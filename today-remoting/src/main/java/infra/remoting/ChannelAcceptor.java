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

package infra.remoting;

import org.reactivestreams.Publisher;

import java.util.function.Function;

import infra.remoting.error.SetupException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * This interface provides the contract where a client or server
 * handles the {@code setup} for a new connection and creates a responder {@code
 * Channel} for accepting requests from the remote peer.
 */
public interface ChannelAcceptor {

  /**
   * Handle the {@code SETUP} frame for a new connection and create a responder {@code Channel} for
   * handling requests from the remote peer.
   *
   * @param setup the {@code setup} received from a client in a server scenario, or in a client
   * scenario this is the setup about to be sent to the server.
   * @param channel channel for sending requests to the remote peer.
   * @return {@code Channel} to accept requests with.
   * @throws SetupException If the acceptor needs to reject the setup of this channel.
   */
  Mono<Channel> accept(ConnectionSetupPayload setup, Channel channel);

  /**
   * Create a {@code ChannelAcceptor} that handles requests with the given {@code Channel}.
   */
  static ChannelAcceptor with(Channel channel) {
    return (setup, sendingChannel) -> Mono.just(channel);
  }

  /** Create a {@code ChannelAcceptor} for fire-and-forget interactions with the given handler. */
  static ChannelAcceptor forFireAndForget(Function<Payload, Mono<Void>> handler) {
    return with(
            new Channel() {
              @Override
              public Mono<Void> fireAndForget(Payload payload) {
                return handler.apply(payload);
              }
            });
  }

  /** Create a {@code ChannelAcceptor} for request-response interactions with the given handler. */
  static ChannelAcceptor forRequestResponse(Function<Payload, Mono<Payload>> handler) {
    return with(
            new Channel() {
              @Override
              public Mono<Payload> requestResponse(Payload payload) {
                return handler.apply(payload);
              }
            });
  }

  /** Create a {@code ChannelAcceptor} for request-stream interactions with the given handler. */
  static ChannelAcceptor forRequestStream(Function<Payload, Flux<Payload>> handler) {
    return with(
            new Channel() {
              @Override
              public Flux<Payload> requestStream(Payload payload) {
                return handler.apply(payload);
              }
            });
  }

  /** Create a {@code ChannelAcceptor} for request-channel interactions with the given handler. */
  static ChannelAcceptor forRequestChannel(Function<Publisher<Payload>, Flux<Payload>> handler) {
    return with(
            new Channel() {
              @Override
              public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
                return handler.apply(payloads);
              }
            });
  }
}
