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

package infra.cloud.provider;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import infra.beans.factory.SmartInitializingSingleton;
import infra.cloud.registry.ServiceDefinition;
import infra.context.ApplicationContext;
import infra.context.support.ApplicationObjectSupport;
import infra.lang.Nullable;
import infra.stereotype.Service;
import infra.util.ClassUtils;
import infra.util.ExceptionUtils;
import infra.util.ObjectUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/10/19 21:40
 */
public class LocalServiceHolder extends ApplicationObjectSupport implements SmartInitializingSingleton {

  private final InetAddress localHost = ExceptionUtils.sneakyThrow(InetAddress::getLocalHost);

  private String localHostName;

  private final int port;

  private final HashMap<String, Object> localServices = new HashMap<>();

  private final ArrayList<ServiceDefinition> definitions = new ArrayList<>();

  public LocalServiceHolder(int port) {
    this.port = port;
  }

  public void setLocalHostName(String localHostName) {
    this.localHostName = localHostName;
  }

  public ArrayList<ServiceDefinition> getServices() {
    return definitions;
  }

  public int getPort() {
    return port;
  }

  @Nullable
  public Object getService(String serviceName) {
    return localServices.get(serviceName);
  }

  @Override
  public void afterSingletonsInstantiated() {
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

      definition.setHost(localHostName == null ? localHost.getHostAddress() : localHostName);

      definition.setPort(port);
      definition.setName(interfaceToUse.getName());

      logger.info("add service: [{}] to interface: [{}]", service, definition.getName());
      definitions.add(definition);
      localServices.put(interfaceToUse.getName(), service); // register object
    }

  }

}
