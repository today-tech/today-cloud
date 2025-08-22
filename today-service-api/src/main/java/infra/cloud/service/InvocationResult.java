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

import infra.core.AttributeAccessor;
import infra.lang.Nullable;
import infra.util.concurrent.Future;

/**
 * Remote service invocation result
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/9 12:10
 */
public interface InvocationResult extends AttributeAccessor {

  @Nullable
  Object getValue();

  boolean isFailed();

  @Nullable
  Throwable getException();

  InvocationType getType();

  default boolean isRequestResponse() {
    return getType() == InvocationType.REQUEST_RESPONSE;
  }

  default boolean isStreaming() {
    return !getType().serverSendsOneMessage();
  }

  /**
   * Only for {@link InvocationType#REQUEST_RESPONSE}
   *
   * @see InvocationType#REQUEST_RESPONSE
   * @see #isRequestResponse()
   */
  Future<Object> future();

  Publisher<Object> publisher();

}
