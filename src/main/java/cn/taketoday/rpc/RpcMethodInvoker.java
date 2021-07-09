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

import cn.taketoday.context.utils.Assert;
import cn.taketoday.rpc.registry.ServiceDefinition;
import cn.taketoday.rpc.serialize.JdkSerialization;
import cn.taketoday.rpc.serialize.Serialization;

/**
 * @author TODAY 2021/7/4 01:58
 */
public abstract class RpcMethodInvoker {

  private Serialization<RpcResponse> serialization = new JdkSerialization<>();
  private RemoteExceptionHandler exceptionHandler = new SimpleRemoteExceptionHandler();

  public Object invoke(ServiceDefinition definition, Method method, Object[] args) throws Throwable {
    // pre
    preProcess(definition, method, args);
    // process
    RpcResponse ret = doInvoke(definition, method, args);
    // post
    return postProcess(definition, ret)
            .getResult();
  }

  protected abstract RpcResponse doInvoke(
          ServiceDefinition definition, Method method, Object[] args) throws IOException;

  protected void preProcess(ServiceDefinition definition, Method method, Object[] args) {
    // no-op
  }

  protected RpcResponse postProcess(ServiceDefinition definition, RpcResponse response) throws Throwable {
    final Throwable exception = response.getException();
    if (exception != null) {
      return exceptionHandler.handle(definition, response);
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

  public void setSerialization(Serialization<RpcResponse> serialization) {
    Assert.notNull(exceptionHandler, "serialization most not be null");
    this.serialization = serialization;
  }

  public Serialization<RpcResponse> getSerialization() {
    return serialization;
  }
}
