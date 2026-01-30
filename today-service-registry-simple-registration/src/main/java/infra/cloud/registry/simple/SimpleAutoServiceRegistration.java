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

import java.util.List;

import infra.cloud.provider.ServicesProvider;
import infra.cloud.registry.AbstractAutoServiceRegistration;
import infra.cloud.registry.RegistrationFactory;
import infra.cloud.registry.RegistrationLifecycle;
import infra.cloud.registry.ServiceRegistry;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/23 21:50
 */
public class SimpleAutoServiceRegistration extends AbstractAutoServiceRegistration<HttpRegistration, Status> {

  private final HttpRegistrationFactory registrationFactory;

  private final SimpleRegistryProperties properties;

  public SimpleAutoServiceRegistration(ServiceRegistry<HttpRegistration, Status> serviceRegistry,
          ServicesProvider servicesProvider, HttpRegistrationFactory registrationFactory,
          SimpleRegistryProperties properties, List<RegistrationLifecycle<HttpRegistration>> registrationLifecycles) {
    super(serviceRegistry, registrationLifecycles, servicesProvider);
    this.registrationFactory = registrationFactory;
    this.properties = properties;
  }

  @Override
  protected Object getConfiguration() {
    return properties;
  }

  @Override
  protected RegistrationFactory<HttpRegistration> getRegistrationFactory() {
    return registrationFactory;
  }

  @Override
  protected boolean isEnabled() {
    return properties.isEnabled();
  }

}
