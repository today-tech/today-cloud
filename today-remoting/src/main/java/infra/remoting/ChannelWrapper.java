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

import java.util.Objects;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Wrapper for a Channel. This is useful when we want to override a specific method.
 */
public class ChannelWrapper implements Channel {

  protected final Channel delegate;

  public ChannelWrapper(Channel delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate is required");
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
