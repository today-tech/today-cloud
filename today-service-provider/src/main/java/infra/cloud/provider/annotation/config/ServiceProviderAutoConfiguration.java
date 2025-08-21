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

package infra.cloud.provider.annotation.config;

import infra.cloud.client.annotation.ConditionalOnDiscoveryEnabled;
import infra.cloud.net.InetProperties;
import infra.cloud.net.InetService;
import infra.cloud.provider.DefaultServiceInterfaceMetadataProvider;
import infra.cloud.provider.LocalServiceHolder;
import infra.cloud.provider.ServiceChannelHandler;
import infra.cloud.registry.annotation.config.AutoServiceRegistrationAutoConfiguration;
import infra.cloud.service.PackageInfoServiceMetadataProvider;
import infra.cloud.service.ServiceInterfaceMetadataProvider;
import infra.cloud.service.ServiceMetadataProvider;
import infra.cloud.service.ServiceMethod;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.stereotype.Component;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/10 22:31
 */
@ConditionalOnDiscoveryEnabled
@EnableConfigurationProperties(InetProperties.class)
@DisableDIAutoConfiguration(before = AutoServiceRegistrationAutoConfiguration.class)
public class ServiceProviderAutoConfiguration {

  @Component
  public static LocalServiceHolder localServiceHolder() {
    return new LocalServiceHolder();
  }

  @Component
  @ConditionalOnMissingBean
  public static ServiceMetadataProvider serviceMetadataProvider() {
    return new PackageInfoServiceMetadataProvider();
  }

  @Component
  @ConditionalOnMissingBean
  public static ServiceInterfaceMetadataProvider<ServiceMethod> serviceInterfaceMetadataProvider(ServiceMetadataProvider serviceMetadataProvider) {
    return new DefaultServiceInterfaceMetadataProvider(serviceMetadataProvider);
  }

  @Component
  public static InetService inetService(InetProperties inetProperties) {
    return new InetService(inetProperties);
  }

  @Component
  public static ServiceChannelHandler serviceChannelHandler() {
    return new ServiceChannelHandler();
  }

}
