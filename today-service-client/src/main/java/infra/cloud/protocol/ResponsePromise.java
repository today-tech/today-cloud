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

package infra.cloud.protocol;

import infra.cloud.RpcRequest;
import infra.util.concurrent.Promise;
import io.netty.util.concurrent.EventExecutor;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/12/22 23:57
 */
public class ResponsePromise extends Promise<Object> {

  private final int requestId;

  private final RpcRequest rpcRequest;

  public ResponsePromise(int requestId, EventExecutor executor, RpcRequest rpcRequest) {
    super(executor);
    this.requestId = requestId;
    this.rpcRequest = rpcRequest;
  }

  public int getRequestId() {
    return requestId;
  }

  public RpcRequest getRpcRequest() {
    return rpcRequest;
  }

}
