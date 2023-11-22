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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import cn.taketoday.cloud.registry.ServiceNotFoundException;

/**
 * @author TODAY 2021/7/4 22:58
 */
public class JdkServiceProxy implements ServiceProxy {

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getProxy(Class<T> serviceInterface, DiscoveryClient discoveryClient, ServiceMethodInvoker rpcInvoker) {
    return (T) Proxy.newProxyInstance(serviceInterface.getClassLoader(), new Class[] { serviceInterface },
            new ServiceInvocationHandler(serviceInterface, discoveryClient, rpcInvoker));
  }

  static final class ServiceInvocationHandler implements InvocationHandler {
    private final CopyOnWriteArrayList<ServiceInstance> serviceInstances = new CopyOnWriteArrayList<>();

    private final Class<?> serviceInterface;

    private final DiscoveryClient discoveryClient;

    private final ServiceMethodInvoker rpcInvoker;

    public ServiceInvocationHandler(Class<?> serviceInterface, DiscoveryClient discoveryClient, ServiceMethodInvoker rpcInvoker) {
      this.serviceInterface = serviceInterface;
      this.discoveryClient = discoveryClient;
      this.rpcInvoker = rpcInvoker;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      CopyOnWriteArrayList<ServiceInstance> serviceInstances = this.serviceInstances;
      if (serviceInstances.isEmpty()) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceInterface.getName());
        serviceInstances.addAll(instances);
        if (serviceInstances.isEmpty()) {
          throw new ServiceNotFoundException(serviceInterface);
        }
      }
      return rpcInvoker.invoke(serviceInstances, method, args);
    }
  }
}
