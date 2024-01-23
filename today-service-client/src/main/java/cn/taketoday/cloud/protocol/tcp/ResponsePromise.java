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

package cn.taketoday.cloud.protocol.tcp;

import cn.taketoday.util.concurrent.FutureListener;
import cn.taketoday.util.concurrent.ListenableFuture;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/12/22 23:57
 */
public class ResponsePromise extends DefaultPromise<Object> implements Promise<Object>, ListenableFuture<Object> {

  private final int requestId;

  public ResponsePromise(int requestId, EventExecutor executor) {
    super(executor);
    this.requestId = requestId;
  }

  public int getRequestId() {
    return requestId;
  }

  @Override
  public void addListener(FutureListener<? super Object> listener) {
    addListener(future -> {
      if (future.isSuccess()) {
        Object now = future.getNow();
        listener.onSuccess(now);
      }
      else {
        Throwable cause = future.cause();
        if (cause != null) {
          listener.onFailure(cause);
        }
      }
    });
  }

}
