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

package cn.taketoday.cloud.provider;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.taketoday.beans.factory.SmartInitializingSingleton;
import cn.taketoday.cloud.registry.ServiceDefinition;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.aware.ApplicationContextSupport;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Service;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/10/19 21:40
 */
public class LocalServiceHolder extends ApplicationContextSupport implements SmartInitializingSingleton {

  InetAddress localHost;

  {
    try {
      localHost = InetAddress.getLocalHost();
    }
    catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }

  private String localHostName;

  private int port;
  private final HashMap<String, Object> localServices = new HashMap<>();
  private final ArrayList<ServiceDefinition> definitions = new ArrayList<>();

  public void setLocalHostName(String localHostName) {
    this.localHostName = localHostName;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public ArrayList<ServiceDefinition> getServices() {
    return definitions;
  }

  @Nullable
  public Object getService(String serviceName) {
    return localServices.get(serviceName);
  }

  @Override
  public void afterSingletonsInstantiated() {
    Assert.state(localHostName != null, "'local-host-address' is required");
    ApplicationContext context = obtainApplicationContext();
    List<Object> services = context.getAnnotatedBeans(Service.class);
    for (Object service : services) {
      Class<Object> serviceImpl = ClassUtils.getUserClass(service);
      Class<?>[] interfaces = serviceImpl.getInterfaces();
      if (ObjectUtils.isEmpty(interfaces)) {
        continue;
      }

      Class<?> interfaceToUse = interfaces[0];
      if (interfaces.length > 1) {
        for (final Class<?> anInterface : interfaces) {
          if (anInterface.isAnnotationPresent(Service.class)) {
            interfaceToUse = anInterface;
            break;
          }
        }
      }

      ServiceDefinition definition = new ServiceDefinition();

      definition.setHost(localHostName);

      definition.setPort(port);
      definition.setName(interfaceToUse.getName());
      definition.setServiceInterface(interfaceToUse);

      log.info("add service: [{}] to interface: [{}]", service, definition.getName());
      definitions.add(definition);
      localServices.put(interfaceToUse.getName(), service); // register object
    }

  }

}
