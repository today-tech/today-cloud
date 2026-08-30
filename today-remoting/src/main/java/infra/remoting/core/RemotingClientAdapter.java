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
