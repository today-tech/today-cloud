/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.rpc;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import cn.taketoday.context.utils.Assert;
import cn.taketoday.rpc.registry.RandomServiceSelector;
import cn.taketoday.rpc.registry.ServiceDefinition;
import cn.taketoday.rpc.registry.ServiceSelector;
import cn.taketoday.rpc.serialize.Serialization;

/**
 * Service Method Invoker
 *
 * @author TODAY 2021/7/4 01:58
 */
public abstract class ServiceMethodInvoker {
  private final Serialization<RpcResponse> serialization;

  private ServiceSelector serviceSelector = new RandomServiceSelector();
  private RemoteExceptionHandler exceptionHandler = new SimpleRemoteExceptionHandler();

  protected ServiceMethodInvoker(Serialization<RpcResponse> serialization) {
    Assert.notNull(serialization, "serialization most not be null");
    this.serialization = serialization;
  }

  public Object invoke(List<ServiceDefinition> definitions, Method method, Object[] args) throws Throwable {
    // pre
    preProcess(definitions, method, args);
    // process
    RpcResponse response = doInvoke(definitions, method, args);
    // post
    return postProcess(definitions, response)
            .getResult();
  }

  protected RpcResponse doInvoke(
          List<ServiceDefinition> definitions, Method method, Object[] args) throws Throwable {
    final ServiceSelector serviceSelector = getServiceSelector();
    final ServiceDefinition selected = serviceSelector.select(definitions);
    try {
      return invokeInternal(selected, method, args);
    }
    catch (Throwable e) {
      return handleInvocationException(e, definitions, selected, method, args);
    }
  }

  protected RpcResponse handleInvocationException(
          Throwable e, List<ServiceDefinition> definitions,
          ServiceDefinition selected, Method method, Object[] args) throws Throwable {
    throw e;
  }

  protected abstract RpcResponse invokeInternal(
          ServiceDefinition selected, Method method, Object[] args) throws IOException, ClassNotFoundException;

  protected void preProcess(List<ServiceDefinition> definitions, Method method, Object[] args) {
    // no-op
  }

  protected RpcResponse postProcess(List<ServiceDefinition> definitions, RpcResponse response) throws Throwable {
    final Throwable serviceException = response.getException();
    if (serviceException != null) {
      return exceptionHandler.handle(definitions, response);
    }
    return response;
  }

  public void setExceptionHandler(RemoteExceptionHandler exceptionHandler) {
    Assert.notNull(exceptionHandler, "exceptionHandler most not be null");
    this.exceptionHandler = exceptionHandler;
  }

  public RemoteExceptionHandler getExceptionHandler() {
    return exceptionHandler;
  }

  public Serialization<RpcResponse> getSerialization() {
    return serialization;
  }

  public void setServiceSelector(ServiceSelector serviceSelector) {
    Assert.notNull(serviceSelector, "serviceSelector most not be null");
    this.serviceSelector = serviceSelector;
  }

  public ServiceSelector getServiceSelector() {
    return serviceSelector;
  }
}
