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

import org.reactivestreams.Publisher;

import infra.remoting.Channel;
import infra.remoting.Payload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Simple adapter from {@link Channel} to {@link RemotingClient}. This is useful in code that needs
 * to deal with both in the same way. When connecting to a server, typically {@link RemotingClient}
 * is expected to be used, but in a responder (client or server), it is necessary to interact with
 * {@link Channel} to make requests to the remote end.
 */
class RemotingClientAdapter implements RemotingClient {

  private final Channel rsocket;

  public RemotingClientAdapter(Channel rsocket) {
    this.rsocket = rsocket;
  }

  public Channel rsocket() {
    return rsocket;
  }

  @Override
  public boolean connect() {
    throw new UnsupportedOperationException("Connect does not apply to a server side RSocket");
  }

  @Override
  public Mono<Channel> source() {
    return Mono.just(rsocket);
  }

  @Override
  public Mono<Void> onClose() {
    return rsocket.onClose();
  }

  @Override
  public Mono<Void> fireAndForget(Mono<Payload> payloadMono) {
    return payloadMono.flatMap(rsocket::fireAndForget);
  }

  @Override
  public Mono<Payload> requestResponse(Mono<Payload> payloadMono) {
    return payloadMono.flatMap(rsocket::requestResponse);
  }

  @Override
  public Flux<Payload> requestStream(Mono<Payload> payloadMono) {
    return payloadMono.flatMapMany(rsocket::requestStream);
  }

  @Override
  public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
    return rsocket.requestChannel(payloads);
  }

  @Override
  public Mono<Void> metadataPush(Mono<Payload> payloadMono) {
    return payloadMono.flatMap(rsocket::metadataPush);
  }

  @Override
  public void dispose() {
    rsocket.dispose();
  }
}
