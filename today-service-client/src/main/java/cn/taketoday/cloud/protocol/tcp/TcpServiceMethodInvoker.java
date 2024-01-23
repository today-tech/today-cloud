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

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Duration;

import cn.taketoday.cloud.RpcRequest;
import cn.taketoday.cloud.RpcResponse;
import cn.taketoday.cloud.ServiceInstance;
import cn.taketoday.cloud.ServiceMethodInvoker;
import cn.taketoday.cloud.ServiceTimeoutException;
import cn.taketoday.cloud.core.serialize.Serialization;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.concurrent.ListenableFuture;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/12/1 20:42
 */
public class TcpServiceMethodInvoker extends ServiceMethodInvoker {

  private final NettyClient client;

  private Duration requestTimeout = Duration.ofSeconds(10);

  public TcpServiceMethodInvoker(Serialization<RpcResponse> serialization) {
    this.client = new NettyClient(serialization);
  }

  public void setRequestTimeout(Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
  }

  @Override
  protected ListenableFuture<Object> invokeInternal(ServiceInstance selected, Method method, Object[] args) throws IOException {
    RpcRequest rpcRequest = new RpcRequest();
    rpcRequest.setMethod(method.getName());
    rpcRequest.setServiceName(selected.getServiceId());
    rpcRequest.setParameterTypes(method.getParameterTypes());
    rpcRequest.setArguments(args);

    return client.write(selected, rpcRequest, requestTimeout);
  }

  protected ListenableFuture<Object> invokeInternal0(ServiceInstance selected, Method method, Object[] args) throws IOException {
    RpcRequest rpcRequest = new RpcRequest();
    rpcRequest.setMethod(method.getName());
    rpcRequest.setServiceName(selected.getServiceId());
    rpcRequest.setParameterTypes(method.getParameterTypes());
    rpcRequest.setArguments(args);

    ResponsePromise responsePromise = client.write(selected, rpcRequest, requestTimeout);

    try {
      responsePromise.await(requestTimeout.toMillis());
    }
    catch (InterruptedException e) {
      throw ExceptionUtils.sneakyThrow(e);
    }

    if (responsePromise.isSuccess()) {
      return responsePromise;
    }

    if (!responsePromise.isDone()) {
      throw new ServiceTimeoutException("Remoting invoke timeout", null);
    }

    Throwable cause = responsePromise.cause();
    throw new ServiceTimeoutException("Remoting invoke failed", cause);
  }

}
