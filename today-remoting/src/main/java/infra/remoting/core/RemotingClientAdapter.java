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

  private final Channel channel;

  public RemotingClientAdapter(Channel channel) {
    this.channel = channel;
  }

  public Channel channel() {
    return channel;
  }

  @Override
  public boolean connect() {
    throw new UnsupportedOperationException("Connect does not apply to a server side Channel");
  }

  @Override
  public Mono<Channel> source() {
    return Mono.just(channel);
  }

  @Override
  public Mono<Void> onClose() {
    return channel.onClose();
  }

  @Override
  public Mono<Void> fireAndForget(Mono<Payload> payloadMono) {
    return payloadMono.flatMap(channel::fireAndForget);
  }

  @Override
  public Mono<Payload> requestResponse(Mono<Payload> payloadMono) {
    return payloadMono.flatMap(channel::requestResponse);
  }

  @Override
  public Flux<Payload> requestStream(Mono<Payload> payloadMono) {
    return payloadMono.flatMapMany(channel::requestStream);
  }

  @Override
  public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
    return channel.requestChannel(payloads);
  }

  @Override
  public Mono<Void> metadataPush(Mono<Payload> payloadMono) {
    return payloadMono.flatMap(channel::metadataPush);
  }

  @Override
  public void dispose() {
    channel.dispose();
  }
}
