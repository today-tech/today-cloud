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

import infra.cloud.provider.ServiceServerProperties;
import infra.cloud.registry.RegistrationFactory;
import infra.cloud.service.ServiceMetadata;
import infra.util.StringUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/24 22:26
 */
public class HttpRegistrationFactory implements RegistrationFactory<HttpRegistration> {

  private final ServiceServerProperties serviceServerProperties;

  private final SimpleRegistryProperties registryProperties;

  public HttpRegistrationFactory(ServiceServerProperties serviceServerProperties, SimpleRegistryProperties registryProperties) {
    this.serviceServerProperties = serviceServerProperties;
    this.registryProperties = registryProperties;
  }

  @Override
  public HttpRegistration createRegistration(ServiceMetadata serviceMetadata) {
    String host = registryProperties.getInstanceHost();
    if (!StringUtils.hasText(host)) {
      throw new IllegalStateException("instanceHost must not be empty");
    }

    HttpRegistration registration = new HttpRegistration();
    registration.setStatus(Status.UP);
    registration.setHost(host);

    registration.setServiceId(serviceMetadata.getId());
    registration.setPort(serviceServerProperties.getPort());
    registration.setInstanceId(registryProperties.getInstanceId());
    if (registration.getServiceId() == null) {
      registration.setDefaultInstanceId();
    }
    return registration;
  }

}
