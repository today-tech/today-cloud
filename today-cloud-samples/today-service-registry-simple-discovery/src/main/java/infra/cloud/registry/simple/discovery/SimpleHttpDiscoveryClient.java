/*
 * Copyright 2021 - 2026 the TODAY authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
