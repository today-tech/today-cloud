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

package cn.taketoday.cloud.registry;

import java.util.Collections;
import java.util.List;

/**
 * @author TODAY 2021/7/4 23:12
 */
public interface ServiceRegistry {

  default RegisteredStatus register(ServiceDefinition definition) {
    return register(Collections.singletonList(definition));
  }

  /**
   * @param definitions services
   * @throws ServiceRegisterFailedException If register failed
   */
  RegisteredStatus register(List<ServiceDefinition> definitions);

  default void unregister(ServiceDefinition definition) {
    unregister(Collections.singletonList(definition));
  }

  void unregister(List<ServiceDefinition> definitions);

  List<ServiceDefinition> lookup(String name);

  List<ServiceDefinition> lookup(Class<?> serviceInterface);

}
