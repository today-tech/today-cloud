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

package infra.remoting;

import org.reactivestreams.Publisher;

import java.util.function.Function;

import infra.remoting.exceptions.SetupException;
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
   * @param sendingSocket socket for sending requests to the remote peer.
   * @return {@code Channel} to accept requests with.
   * @throws SetupException If the acceptor needs to reject the setup of this socket.
   */
  Mono<Channel> accept(ConnectionSetupPayload setup, Channel sendingSocket);

  /**
   * Create a {@code SocketAcceptor} that handles requests with the given {@code Channel}.
   */
  static ChannelAcceptor with(Channel channel) {
    return (setup, sendingChannel) -> Mono.just(channel);
  }

  /** Create a {@code SocketAcceptor} for fire-and-forget interactions with the given handler. */
  static ChannelAcceptor forFireAndForget(Function<Payload, Mono<Void>> handler) {
    return with(
            new Channel() {
              @Override
              public Mono<Void> fireAndForget(Payload payload) {
                return handler.apply(payload);
              }
            });
  }

  /** Create a {@code SocketAcceptor} for request-response interactions with the given handler. */
  static ChannelAcceptor forRequestResponse(Function<Payload, Mono<Payload>> handler) {
    return with(
            new Channel() {
              @Override
              public Mono<Payload> requestResponse(Payload payload) {
                return handler.apply(payload);
              }
            });
  }

  /** Create a {@code SocketAcceptor} for request-stream interactions with the given handler. */
  static ChannelAcceptor forRequestStream(Function<Payload, Flux<Payload>> handler) {
    return with(
            new Channel() {
              @Override
              public Flux<Payload> requestStream(Payload payload) {
                return handler.apply(payload);
              }
            });
  }

  /** Create a {@code SocketAcceptor} for request-channel interactions with the given handler. */
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
