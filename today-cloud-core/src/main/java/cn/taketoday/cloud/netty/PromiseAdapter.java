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

package cn.taketoday.cloud.netty;

import cn.taketoday.util.concurrent.Future;
import cn.taketoday.util.concurrent.Promise;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/5/23 22:34
 */
public class PromiseAdapter<V> implements GenericFutureListener<io.netty.util.concurrent.Future<V>> {

  private final Promise<V> settable;

  PromiseAdapter(Promise<V> settable) {
    this.settable = settable;
  }

  @Override
  public void operationComplete(io.netty.util.concurrent.Future<V> future) {
    Throwable cause = future.cause();
    if (cause != null) {
      settable.tryFailure(cause);
    }
    else {
      settable.trySuccess(future.getNow());
    }
  }

  public static <T> Future<T> adapt(io.netty.util.concurrent.Future<T> future) {
    return adapt(future, Future.forPromise());
  }

  public static <T> Future<T> adapt(io.netty.util.concurrent.Future<T> future, Promise<T> settable) {
    future.addListener(new PromiseAdapter<>(settable));
    return settable;
  }
}

