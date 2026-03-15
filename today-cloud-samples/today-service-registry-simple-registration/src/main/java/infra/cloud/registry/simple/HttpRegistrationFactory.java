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
    if (StringUtils.isBlank(host)) {
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

    registration.setDefaultInstanceId();
    registration.getMetadata().putAll(serviceMetadata.getProperties());
    return registration;
  }

}
