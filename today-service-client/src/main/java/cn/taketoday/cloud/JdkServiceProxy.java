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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CopyOnWriteArrayList;

import cn.taketoday.cloud.registry.ServiceDefinition;
import cn.taketoday.cloud.registry.ServiceNotFoundException;
import cn.taketoday.cloud.registry.ServiceRegistry;

/**
 * @author TODAY 2021/7/4 22:58
 */
public class JdkServiceProxy implements ServiceProxy {

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getProxy(Class<T> serviceInterface,
          ServiceRegistry serviceRegistry, ServiceMethodInvoker rpcInvoker) {

    final class ServiceInvocationHandler implements InvocationHandler {
      final CopyOnWriteArrayList<ServiceDefinition> definitions = new CopyOnWriteArrayList<>();

      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        final CopyOnWriteArrayList<ServiceDefinition> definitions = this.definitions;
        if (definitions.isEmpty()) {
          definitions.addAll(serviceRegistry.lookup(serviceInterface));
          if (definitions.isEmpty()) {
            throw new ServiceNotFoundException(serviceInterface);
          }
        }
        return rpcInvoker.invoke(definitions, method, args);
      }
    }

    return (T) Proxy.newProxyInstance(
            serviceInterface.getClassLoader(), new Class[] { serviceInterface }, new ServiceInvocationHandler());
  }
}
