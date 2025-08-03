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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Package private class with default implementations for use in {@link Channel}. The main purpose
 * is to hide static {@link UnsupportedOperationException} declarations.
 */
class ChannelAdapter {

  private static final Mono<Void> UNSUPPORTED_FIRE_AND_FORGET =
          Mono.error(new UnsupportedInteractionException("Fire-and-Forget"));

  private static final Mono<Payload> UNSUPPORTED_REQUEST_RESPONSE =
          Mono.error(new UnsupportedInteractionException("Request-Response"));

  private static final Flux<Payload> UNSUPPORTED_REQUEST_STREAM =
          Flux.error(new UnsupportedInteractionException("Request-Stream"));

  private static final Flux<Payload> UNSUPPORTED_REQUEST_CHANNEL =
          Flux.error(new UnsupportedInteractionException("Request-Channel"));

  private static final Mono<Void> UNSUPPORTED_METADATA_PUSH =
          Mono.error(new UnsupportedInteractionException("Metadata-Push"));

  static Mono<Void> fireAndForget(Payload payload) {
    payload.release();
    return ChannelAdapter.UNSUPPORTED_FIRE_AND_FORGET;
  }

  static Mono<Payload> requestResponse(Payload payload) {
    payload.release();
    return ChannelAdapter.UNSUPPORTED_REQUEST_RESPONSE;
  }

  static Flux<Payload> requestStream(Payload payload) {
    payload.release();
    return ChannelAdapter.UNSUPPORTED_REQUEST_STREAM;
  }

  static Flux<Payload> requestChannel(Publisher<Payload> payloads) {
    return ChannelAdapter.UNSUPPORTED_REQUEST_CHANNEL;
  }

  static Mono<Void> metadataPush(Payload payload) {
    payload.release();
    return ChannelAdapter.UNSUPPORTED_METADATA_PUSH;
  }

  private static class UnsupportedInteractionException extends RuntimeException {

    private static final long serialVersionUID = 5084623297446471999L;

    UnsupportedInteractionException(String interactionName) {
      super(interactionName + " not implemented.", null, false, false);
    }
  }
}
