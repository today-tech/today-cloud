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

package infra.cloud.registry.simple.config;

import infra.annotation.config.web.client.RestClientAutoConfiguration;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.config.EmbeddedValueResolver;
import infra.cloud.client.annotation.ConditionalOnDiscoveryEnabled;
import infra.cloud.registry.simple.api.SimpleHttpServiceRegistryAPI;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.stereotype.Component;
import infra.web.client.RestClient;
import infra.web.client.support.RestClientAdapter;
import infra.web.service.invoker.HttpServiceProxyFactory;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/24 17:44
 */
@ConditionalOnDiscoveryEnabled
@EnableConfigurationProperties(SimpleHttpProperties.class)
@DisableDIAutoConfiguration(after = RestClientAutoConfiguration.class)
public class SimpleHttpServiceRegistryAPIAutoConfiguration {

  @Component
  @ConditionalOnMissingBean
  public static SimpleHttpServiceRegistryAPI simpleHttpServiceRegistryAPI(
          ConfigurableBeanFactory factory, SimpleHttpProperties properties, RestClient.Builder builder) {
    RestClientAdapter adapter = RestClientAdapter.create(builder.baseURI(properties.getUri()).build());
    return HttpServiceProxyFactory.forAdapter(adapter)
            .embeddedValueResolver(new EmbeddedValueResolver(factory))
            .build()
            .createClient(SimpleHttpServiceRegistryAPI.class);
  }

}
