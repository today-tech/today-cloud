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

package infra.cloud.registry.simple.discovery;

import java.util.ArrayList;
import java.util.List;

import infra.cloud.client.DiscoveryClient;
import infra.cloud.client.ServiceInstance;
import infra.cloud.registry.simple.HttpRegistration;
import infra.cloud.registry.simple.Status;
import infra.cloud.registry.simple.api.SimpleHttpServiceRegistryAPI;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/24 16:42
 */
public class SimpleHttpDiscoveryClient implements DiscoveryClient {

  private final SimpleHttpServiceRegistryAPI serviceRegistryAPI;

  public SimpleHttpDiscoveryClient(SimpleHttpServiceRegistryAPI serviceRegistryAPI) {
    this.serviceRegistryAPI = serviceRegistryAPI;
  }

  @Override
  public String getDescription() {
    return "Simple Http DiscoveryClient";
  }

  @Override
  public List<ServiceInstance> getInstances(String serviceId) {
    List<HttpRegistration> registrations = serviceRegistryAPI.lookup(serviceId);
    List<ServiceInstance> instances = new ArrayList<>(registrations.size());
    for (HttpRegistration registration : registrations) {
      if (registration.getStatus() == Status.UP) {
        instances.add(registration);
      }
    }
    return instances;
  }

  @Override
  public List<String> getServices() {
    return new ArrayList<>(serviceRegistryAPI.services().keySet());
  }

}
