/*
 * Copyright 2021 - 2023 the original author or authors.
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

package cn.taketoday.cloud;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import cn.taketoday.cloud.core.ServiceInstance;
import cn.taketoday.cloud.registry.RandomServiceSelector;
import cn.taketoday.cloud.registry.ServiceSelector;
import cn.taketoday.lang.Assert;

/**
 * Service Method Invoker
 *
 * @author TODAY 2021/7/4 01:58
 */
public abstract class ServiceMethodInvoker {

  protected ServiceSelector serviceSelector = new RandomServiceSelector();

  protected RemoteExceptionHandler exceptionHandler = new SimpleRemoteExceptionHandler();

  public Object invoke(List<ServiceInstance> instances, Method method, Object[] args) throws Throwable {
    // pre
    preProcess(instances, method, args);
    // process
    RpcResponse response = doInvoke(instances, method, args);
    // post
    return postProcess(instances, response)
            .getResult();
  }

  protected RpcResponse doInvoke(List<ServiceInstance> instances, Method method, Object[] args) throws Throwable {
    final ServiceInstance selected = serviceSelector.select(instances);
    try {
      return invokeInternal(selected, method, args);
    }
    catch (Throwable e) {
      return handleInvocationException(e, instances, selected, method, args);
    }
  }

  protected RpcResponse handleInvocationException(
          Throwable e, List<ServiceInstance> instances,
          ServiceInstance selected, Method method, Object[] args) throws Throwable {
    throw e;
  }

  protected abstract RpcResponse invokeInternal(
          ServiceInstance selected, Method method, Object[] args) throws IOException, ClassNotFoundException;

  protected void preProcess(List<ServiceInstance> instances, Method method, Object[] args) {
    // no-op
  }

  protected RpcResponse postProcess(List<ServiceInstance> instances, RpcResponse response) throws Throwable {
    final Throwable serviceException = response.getException();
    if (serviceException != null) {
      return exceptionHandler.handle(instances, response);
    }
    return response;
  }

  public void setExceptionHandler(RemoteExceptionHandler exceptionHandler) {
    Assert.notNull(exceptionHandler, "exceptionHandler is required");
    this.exceptionHandler = exceptionHandler;
  }

  public RemoteExceptionHandler getExceptionHandler() {
    return exceptionHandler;
  }

  public void setServiceSelector(ServiceSelector serviceSelector) {
    Assert.notNull(serviceSelector, "serviceSelector is required");
    this.serviceSelector = serviceSelector;
  }

  public ServiceSelector getServiceSelector() {
    return serviceSelector;
  }
}
