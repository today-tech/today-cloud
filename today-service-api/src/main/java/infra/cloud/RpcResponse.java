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

package infra.cloud;

import java.io.Serial;
import java.io.Serializable;

import infra.lang.Nullable;

/**
 * @author TODAY 2021/7/4 22:31
 */
public class RpcResponse implements Serializable {

  public static final RpcResponse empty = new RpcResponse();

  @Serial
  private static final long serialVersionUID = 1L;

  /** service result */
  private Object result;

  @Nullable
  private Throwable exception;

  @Nullable
  private RpcMethod rpcMethod;

  public RpcResponse() { }

  public RpcResponse(@Nullable RpcMethod rpcMethod, Object result) {
    this.rpcMethod = rpcMethod;
    this.result = result;
  }

  public void setRpcMethod(@Nullable RpcMethod rpcMethod) {
    this.rpcMethod = rpcMethod;
  }

  @Nullable
  public RpcMethod getRpcMethod() {
    return rpcMethod;
  }

  public void setResult(Object result) {
    this.result = result;
  }

  public Object getResult() {
    return result;
  }

  @Nullable
  public Throwable getException() {
    return exception;
  }

  public void setException(@Nullable Throwable exception) {
    this.exception = exception;
  }

  // static

  public static RpcResponse ofThrowable(@Nullable Throwable throwable) {
    final RpcResponse rpcResponse = new RpcResponse();
    rpcResponse.setException(throwable);
    return rpcResponse;
  }

  public static RpcResponse ofThrowable(RpcMethod rpcMethod, @Nullable Throwable throwable) {
    final RpcResponse rpcResponse = new RpcResponse();
    rpcResponse.setException(throwable);
    rpcResponse.setRpcMethod(rpcMethod);
    return rpcResponse;
  }

}
