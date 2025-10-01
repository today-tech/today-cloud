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

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/26 22:07
 */
public class ServiceObject {

  private final Class<?> serviceInterface;

  private final Object instance;

  public ServiceObject(Class<?> serviceInterface, Object instance) {
    this.serviceInterface = serviceInterface;
    this.instance = instance;
  }

  public Class<?> getInterface() {
    return serviceInterface;
  }

  public Object getInstance() {
    return instance;
  }

}
