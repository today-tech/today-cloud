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
