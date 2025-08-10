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

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import infra.beans.factory.SmartInitializingSingleton;
import infra.context.ApplicationContext;
import infra.context.support.ApplicationObjectSupport;
import infra.lang.Nullable;
import infra.stereotype.Service;
import infra.util.ClassUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/10/19 21:40
 */
public class LocalServiceHolder extends ApplicationObjectSupport implements SmartInitializingSingleton {

  private final HashMap<Class<?>, Object> localServices = new HashMap<>();

  @Nullable
  @SuppressWarnings("unchecked")
  public <T> T getService(Class<T> serviceInterface) {
    return (T) localServices.get(serviceInterface);
  }

  @Override
  public void afterSingletonsInstantiated() {
    ApplicationContext context = obtainApplicationContext();
    List<Object> services = context.getAnnotatedBeans(Service.class);

    for (Object service : services) {
      Class<Object> serviceImpl = ClassUtils.getUserClass(service);
      Set<Class<?>> interfaces = ClassUtils.getAllInterfacesForClassAsSet(serviceImpl);
      if (interfaces.isEmpty()) {
        continue;
      }

      for (final Class<?> anInterface : interfaces) {
        if (anInterface.isAnnotationPresent(Service.class)) {
          Object object = localServices.put(anInterface, service);
          if (object != null) {
            throw new IllegalStateException("Service '%s' is already registered: [%s]".formatted(anInterface.getName(), object));
          }
          logger.info("add service: [{}] to interface: [{}]", service, anInterface.getName());
        }
      }
    }

  }

}
