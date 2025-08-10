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

package infra.cloud.registry.simple;

import infra.cloud.registry.ServiceRegistry;
import infra.cloud.registry.simple.api.SimpleHttpServiceRegistryAPI;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/7 20:40
 */
public class SimpleHttpServiceRegistry implements ServiceRegistry<HttpRegistration, String> {

  private final SimpleHttpServiceRegistryAPI serviceRegistryAPI;

  public SimpleHttpServiceRegistry(SimpleHttpServiceRegistryAPI serviceRegistryAPI) {
    this.serviceRegistryAPI = serviceRegistryAPI;
  }

  @Override
  public void register(HttpRegistration registration) {
    serviceRegistryAPI.register(registration);
  }

  @Override
  public void unregister(HttpRegistration registration) {
    serviceRegistryAPI.unregister(registration);
  }

  @Override
  public void close() {

  }

  @Override
  public void setStatus(HttpRegistration registration, String status) {
    registration.setStatus(status);
  }

  @Override
  public String getStatus(HttpRegistration registration) {
    return registration.getStatus();
  }

}
