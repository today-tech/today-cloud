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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.rpc.config.ServiceConfig;

/**
 * @author TODAY 2021/7/3 22:52
 */
public class ServiceInstanceRegistry {
  static Map<Class<?>, Object> local = new HashMap<>();

  /**
   * register to service interface reference to local map
   */
  public static <T> void register(Class<T> serviceInterface, T reference) {
    local.put(serviceInterface, reference);
  }

  public static <T> void register(ServiceConfig<T> config) {
    register(config.getInterface(), config.getReference());
  }

  /**
   * lookup for a target service
   *
   * @param serviceInterface
   *         target service interface
   * @param <T>
   *         service type
   *
   * @return target service interface
   */
  @SuppressWarnings("unchecked")
  public static <T> T lookup(Class<T> serviceInterface) {
    final Object service = local.get(serviceInterface);
    if (service == null) {
      throw new IllegalStateException("cannot found a service: " + serviceInterface);
    }

    final class ServiceInvocationHandler implements InvocationHandler {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return method.invoke(service, args);
      }
    }
    return (T) Proxy.newProxyInstance(
            serviceInterface.getClassLoader(), new Class[] { serviceInterface }, new ServiceInvocationHandler());
  }

}
