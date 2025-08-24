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

package infra.cloud.registry.simple.annotation.config;

import infra.cloud.client.annotation.ConditionalOnDiscoveryEnabled;
import infra.cloud.net.InetProperties;
import infra.cloud.net.InetService;
import infra.cloud.registry.simple.HttpRegistration;
import infra.cloud.registry.simple.SimpleAutoServiceRegistration;
import infra.cloud.registry.simple.SimpleHttpServiceRegistry;
import infra.cloud.registry.simple.SimpleRegistryProperties;
import infra.cloud.registry.simple.Status;
import infra.cloud.registry.simple.api.SimpleHttpServiceRegistryAPI;
import infra.cloud.registry.simple.config.SimpleHttpServiceRegistryAPIAutoConfiguration;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.stereotype.Component;
import infra.util.StringUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/23 21:39
 */
@ConditionalOnDiscoveryEnabled
@EnableConfigurationProperties({ InetProperties.class })
@DisableDIAutoConfiguration(after = SimpleHttpServiceRegistryAPIAutoConfiguration.class)
public class SimpleHttpServiceRegistryAutoConfiguration {

  @Component
  public static SimpleRegistryProperties simpleRegistryProperties(InetService inetService) {
    return new SimpleRegistryProperties(inetService);
  }

  @Component
  @ConditionalOnMissingBean
  public static SimpleHttpServiceRegistry simpleHttpServiceRegistry(SimpleHttpServiceRegistryAPI serviceRegistryAPI) {
    return new SimpleHttpServiceRegistry(serviceRegistryAPI);
  }

  @Component
  public static SimpleAutoServiceRegistration autoServiceRegistration(HttpRegistration httpRegistration,
          SimpleRegistryProperties properties, SimpleHttpServiceRegistry serviceRegistry) {
    return new SimpleAutoServiceRegistration(serviceRegistry, httpRegistration, properties);
  }

  @Component
  @ConditionalOnMissingBean
  public static InetService inetService(InetProperties inetProperties) {
    return new InetService(inetProperties);
  }

  @Component
  public static HttpRegistration httpRegistration(SimpleRegistryProperties properties) {
    String host = properties.getInstanceHost();
    if (!StringUtils.hasText(host)) {
      throw new IllegalStateException("instanceHost must not be empty");
    }

    HttpRegistration registration = new HttpRegistration();
    registration.setStatus(Status.UP);
    registration.setHost(host);
    registration.setPort(properties.getInstancePort());
    registration.setInstanceId(properties.getInstanceId());
    registration.setServiceId(properties.getInstanceId());
    return registration;
  }

}
