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

import infra.beans.factory.ObjectProvider;
import infra.cloud.client.annotation.ConditionalOnDiscoveryEnabled;
import infra.cloud.net.InetProperties;
import infra.cloud.net.InetService;
import infra.cloud.provider.ServiceServerProperties;
import infra.cloud.provider.ServicesProvider;
import infra.cloud.registry.RegistrationLifecycle;
import infra.cloud.registry.simple.HttpRegistration;
import infra.cloud.registry.simple.HttpRegistrationFactory;
import infra.cloud.registry.simple.SimpleAutoServiceRegistration;
import infra.cloud.registry.simple.SimpleHttpServiceRegistry;
import infra.cloud.registry.simple.SimpleRegistryProperties;
import infra.cloud.registry.simple.api.SimpleHttpServiceRegistryAPI;
import infra.cloud.registry.simple.config.SimpleHttpServiceRegistryAPIAutoConfiguration;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.stereotype.Component;

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
  public static SimpleAutoServiceRegistration autoServiceRegistration(
          HttpRegistrationFactory httpRegistrationFactory, ServicesProvider servicesProvider,
          SimpleRegistryProperties properties, SimpleHttpServiceRegistry serviceRegistry,
          ObjectProvider<RegistrationLifecycle<HttpRegistration>> registrationLifecycles) {
    return new SimpleAutoServiceRegistration(serviceRegistry, servicesProvider,
            httpRegistrationFactory, properties, registrationLifecycles.orderedList());
  }

  @Component
  @ConditionalOnMissingBean
  public static InetService inetService(InetProperties inetProperties) {
    return new InetService(inetProperties);
  }

  @Component
  public static HttpRegistrationFactory httpRegistrationFactory(ServiceServerProperties serverProperties, SimpleRegistryProperties properties) {
    return new HttpRegistrationFactory(serverProperties, properties);
  }

}
