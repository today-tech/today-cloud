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

import org.reactivestreams.Subscriber;

import reactor.core.Disposable;
import reactor.core.publisher.Mono;

/**
 * An interface which allows listening to when a specific instance of this interface is closed
 */
public interface Closeable extends Disposable {

  /**
   * Returns a {@link Mono} that terminates when the instance is terminated by any reason. Note, in
   * case of error termination, the cause of error will be propagated as an error signal through
   * {@link org.reactivestreams.Subscriber#onError(Throwable)}. Otherwise, {@link
   * Subscriber#onComplete()} will be called.
   *
   * @return a {@link Mono} to track completion with success or error of the underlying resource.
   * When the underlying resource is an `Channel`, the {@code Mono} exposes stream 0 (i.e.
   * connection level) errors.
   */
  Mono<Void> onClose();

}
