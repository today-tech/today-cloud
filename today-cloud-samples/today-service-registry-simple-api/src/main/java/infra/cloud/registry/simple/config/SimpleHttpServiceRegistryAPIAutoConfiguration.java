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

package infra.cloud.registry.simple.config;

import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.config.EmbeddedValueResolver;
import infra.cloud.client.annotation.ConditionalOnDiscoveryEnabled;
import infra.cloud.registry.simple.api.SimpleHttpServiceRegistryAPI;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.http.service.invoker.HttpServiceProxyFactory;
import infra.http.service.support.RestClientAdapter;
import infra.stereotype.Component;
import infra.web.client.RestClient;
import infra.web.client.config.RestClientAutoConfiguration;

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
