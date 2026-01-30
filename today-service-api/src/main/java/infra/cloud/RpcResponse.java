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

package infra.cloud;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;

import infra.cloud.service.ServiceMethod;

/**
 * @author TODAY 2021/7/4 22:31
 */
public class RpcResponse implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  /** service result */

  @Nullable
  private Object result;

  @Nullable
  private Throwable exception;

  @Nullable
  private ServiceMethod method;

  public RpcResponse() {
  }

  public RpcResponse(@Nullable ServiceMethod method, Object result) {
    this.method = method;
    this.result = result;
  }

  public void setMethod(@Nullable ServiceMethod rpcMethod) {
    this.method = rpcMethod;
  }

  @Nullable
  public ServiceMethod getMethod() {
    return method;
  }

  public void setResult(@Nullable Object result) {
    this.result = result;
  }

  @Nullable
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

}
