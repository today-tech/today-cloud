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

package infra.cloud.client.config;

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
  public static DiscoveryClient primaryDiscoveryClient(ObjectProvider<DiscoveryClient> discoveryClients) {
    var discoveryClientList = discoveryClients.orderedList();
    if (discoveryClientList.size() == 1) {
      return discoveryClientList.get(0);
    }
    return new CompositeDiscoveryClient(discoveryClientList);
  }

  @MissingBean
  public static SimpleDiscoveryClient simpleDiscoveryClient(SimpleDiscoveryProperties properties) {
    return new SimpleDiscoveryClient(properties);
  }

}
