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
