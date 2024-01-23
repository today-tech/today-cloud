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

package cn.taketoday.cloud.protocol.http;

import java.lang.reflect.Method;

import cn.taketoday.cloud.RpcRequest;
import cn.taketoday.cloud.RpcResponse;
import cn.taketoday.cloud.ServiceInstance;
import cn.taketoday.cloud.ServiceMethodInvoker;
import cn.taketoday.scheduling.annotation.AsyncResult;
import cn.taketoday.util.concurrent.ListenableFuture;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2021/7/4 23:10
 */
final class HttpServiceMethodInvoker extends ServiceMethodInvoker {

  private final HttpOperations httpOperations;

  HttpServiceMethodInvoker(HttpOperations httpOperations) {
    this.httpOperations = httpOperations;
  }

  @Override
  protected ListenableFuture<Object> invokeInternal(ServiceInstance selected, Method method, Object[] args) throws Throwable {
    RpcRequest rpcRequest = new RpcRequest();
    rpcRequest.setMethod(method.getName());
    rpcRequest.setServiceName(selected.getServiceId());
    rpcRequest.setParameterTypes(method.getParameterTypes());
    rpcRequest.setArguments(args);
    RpcResponse execute = httpOperations.execute(selected, rpcRequest);
    Throwable exception = execute.getException();
    if (exception != null) {
      return AsyncResult.forExecutionException(exception);
    }
    return AsyncResult.forValue(execute.getResult());
  }

}
