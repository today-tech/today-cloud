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
 * Wrapper for a Channel. This is useful when we want to override a specific method.
 */
public class ChannelWrapper implements Channel {

  protected final Channel delegate;

  public ChannelWrapper(Channel delegate) {
    this.delegate = delegate;
  }

  @Override
  public Mono<Void> fireAndForget(Payload payload) {
    return delegate.fireAndForget(payload);
  }

  @Override
  public Mono<Payload> requestResponse(Payload payload) {
    return delegate.requestResponse(payload);
  }

  @Override
  public Flux<Payload> requestStream(Payload payload) {
    return delegate.requestStream(payload);
  }

  @Override
  public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
    return delegate.requestChannel(payloads);
  }

  @Override
  public Mono<Void> metadataPush(Payload payload) {
    return delegate.metadataPush(payload);
  }

  @Override
  public double availability() {
    return delegate.availability();
  }

  @Override
  public void dispose() {
    delegate.dispose();
  }

  @Override
  public boolean isDisposed() {
    return delegate.isDisposed();
  }

  @Override
  public Mono<Void> onClose() {
    return delegate.onClose();
  }

}
