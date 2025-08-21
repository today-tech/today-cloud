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

package infra.cloud.provider;

import org.reactivestreams.Publisher;

import infra.remoting.Channel;
import infra.remoting.Payload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/21 22:58
 */
public class ServiceChannelHandler implements Channel {

  @Override
  public Mono<Payload> requestResponse(Payload payload) {
    return Channel.super.requestResponse(payload);
  }

  @Override
  public Flux<Payload> requestStream(Payload payload) {
    return Channel.super.requestStream(payload);
  }

  @Override
  public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
    return Channel.super.requestChannel(payloads);
  }

  @Override
  public Mono<Void> fireAndForget(Payload payload) {
    return Channel.super.fireAndForget(payload);
  }

  @Override
  public Mono<Void> metadataPush(Payload payload) {
    return Channel.super.metadataPush(payload);
  }

}
