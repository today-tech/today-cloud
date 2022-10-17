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

package cn.taketoday.cloud;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import cn.taketoday.cloud.registry.ServiceDefinition;
import cn.taketoday.cloud.registry.ServiceNotFoundException;

/**
 * @author TODAY 2021/7/4 22:58
 */
public class JdkServiceProxy implements ServiceProxy {

  @Override
  public <T> Object getProxy(
          Class<T> serviceInterface, Supplier<List<ServiceDefinition>> serviceSupplier, ServiceMethodInvoker rpcInvoker) {

    final class ServiceInvocationHandler implements InvocationHandler {
      final CopyOnWriteArrayList<ServiceDefinition> definitions = new CopyOnWriteArrayList<>(serviceSupplier.get());

      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        final CopyOnWriteArrayList<ServiceDefinition> definitions = this.definitions;
        if (definitions.isEmpty()) {
          definitions.addAll(serviceSupplier.get());
          if (definitions.isEmpty()) {
            throw new ServiceNotFoundException("Cannot found a service: " + serviceInterface);
          }
        }
        return rpcInvoker.invoke(definitions, method, args);
      }
    }

    return Proxy.newProxyInstance(
            serviceInterface.getClassLoader(), new Class[] { serviceInterface }, new ServiceInvocationHandler());
  }
}
