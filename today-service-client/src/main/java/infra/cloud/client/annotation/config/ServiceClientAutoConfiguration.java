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

package infra.cloud.client.annotation.config;

import infra.beans.factory.ObjectProvider;
import infra.cloud.client.DiscoveryClient;
import infra.cloud.client.annotation.ConditionalOnDiscoveryEnabled;
import infra.cloud.client.simple.SimpleDiscoveryProperties;
import infra.cloud.service.ClientInterceptor;
import infra.cloud.service.DefaultServiceInterfaceMetadataProvider;
import infra.cloud.service.DefaultServiceProxyFactory;
import infra.cloud.service.PackageInfoServiceMetadataProvider;
import infra.cloud.service.ReturnValueResolver;
import infra.cloud.service.ServiceInterfaceMetadataProvider;
import infra.cloud.service.ServiceInterfaceMethod;
import infra.cloud.service.ServiceMetadataProvider;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.stereotype.Component;

/**
 * Auto-configuration for remote service client.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/9 22:14
 */
@DisableDIAutoConfiguration
@ConditionalOnDiscoveryEnabled
@EnableConfigurationProperties(SimpleDiscoveryProperties.class)
public class ServiceClientAutoConfiguration {

  @Component
  public static DefaultServiceProxyFactory serviceProxyFactory(
          ServiceInterfaceMetadataProvider<ServiceInterfaceMethod> metadataProvider,
          DiscoveryClient discoveryClient, ObjectProvider<ClientInterceptor> clientInterceptors) {
    return new DefaultServiceProxyFactory(discoveryClient, metadataProvider, clientInterceptors.orderedList());
  }

  @Component
  @ConditionalOnMissingBean
  public static ServiceMetadataProvider serviceMetadataProvider() {
    return new PackageInfoServiceMetadataProvider();
  }

  @Component
  @ConditionalOnMissingBean
  public static ServiceInterfaceMetadataProvider<ServiceInterfaceMethod> serviceInterfaceMetadataProvider(
          ServiceMetadataProvider serviceMetadataProvider, ObjectProvider<ReturnValueResolver> resolvers) {
    return new DefaultServiceInterfaceMetadataProvider(serviceMetadataProvider, resolvers.orderedList());
  }

}
