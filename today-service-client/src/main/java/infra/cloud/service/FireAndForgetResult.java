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

package infra.cloud.service;

import org.reactivestreams.Publisher;

import infra.util.concurrent.Future;
import reactor.core.publisher.Mono;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/15 20:36
 */
class FireAndForgetResult extends AbstractInvocationResult {

  private final Mono fireAndForgetMono;

  public FireAndForgetResult(Mono<Void> fireAndForgetMono) {
    this.fireAndForgetMono = fireAndForgetMono;
  }

  @Override
  public Object getValue() {
    return null;
  }

  @Override
  public boolean isFailed() {
    return false;
  }

  @Override
  public Throwable getException() {
    return null;
  }

  @Override
  public InvocationType getType() {
    return InvocationType.FIRE_AND_FORGET;
  }

  @Override
  public boolean isRequestResponse() {
    return false;
  }

  @Override
  public boolean isStreaming() {
    return false;
  }

  @Override
  public Future<Object> future() {
    return null;
  }

  @Override
  public Publisher<Object> publisher() {
    return fireAndForgetMono;
  }

}
