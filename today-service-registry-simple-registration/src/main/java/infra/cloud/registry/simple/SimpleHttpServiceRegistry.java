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

package infra.cloud.registry.simple;

import infra.cloud.registry.ServiceRegistry;
import infra.cloud.registry.simple.api.SimpleHttpServiceRegistryAPI;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/7 20:40
 */
public class SimpleHttpServiceRegistry implements ServiceRegistry<HttpRegistration, Status> {

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
  public void setStatus(HttpRegistration registration, Status status) {
    registration.setStatus(status);
    serviceRegistryAPI.update(registration);
  }

  @Override
  public Status getStatus(HttpRegistration registration) {
    return registration.getStatus();
  }

}
