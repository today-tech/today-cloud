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
import infra.cloud.client.CompositeDiscoveryClient;
import infra.cloud.client.DiscoveryClient;
import infra.cloud.client.annotation.ConditionalOnDiscoveryEnabled;
import infra.cloud.client.simple.SimpleDiscoveryClient;
import infra.cloud.client.simple.SimpleDiscoveryProperties;
import infra.context.annotation.MissingBean;
import infra.context.annotation.Primary;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.properties.EnableConfigurationProperties;
import infra.stereotype.Component;

/**
 * Auto-configuration for discovery client.
 *
 * @author Biju Kunjummen
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
@DisableDIAutoConfiguration
@ConditionalOnDiscoveryEnabled
@EnableConfigurationProperties(SimpleDiscoveryProperties.class)
public class DiscoveryClientAutoConfiguration {

  @Primary
  @Component
  public static CompositeDiscoveryClient compositeDiscoveryClient(ObjectProvider<DiscoveryClient> discoveryClients) {
    return new CompositeDiscoveryClient(discoveryClients.orderedList());
  }

  @MissingBean
  public static SimpleDiscoveryClient simpleDiscoveryClient(SimpleDiscoveryProperties properties) {
    return new SimpleDiscoveryClient(properties);
  }

}
