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

package infra.cloud.registry.simple.discovery;

import infra.cloud.client.annotation.ConditionalOnDiscoveryEnabled;
import infra.cloud.client.config.DiscoveryClientAutoConfiguration;
import infra.cloud.registry.simple.api.SimpleHttpServiceRegistryAPI;
import infra.cloud.registry.simple.config.SimpleHttpServiceRegistryAPIAutoConfiguration;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.stereotype.Component;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/24 17:40
 */
@ConditionalOnDiscoveryEnabled
@DisableDIAutoConfiguration(before = DiscoveryClientAutoConfiguration.class,
        after = SimpleHttpServiceRegistryAPIAutoConfiguration.class)
public class SimpleHttpDiscoveryClientAutoConfiguration {

  @Component
  public static SimpleHttpDiscoveryClient simpleHttpDiscoveryClient(SimpleHttpServiceRegistryAPI simpleHttpServiceRegistryAPI) {
    return new SimpleHttpDiscoveryClient(simpleHttpServiceRegistryAPI);
  }

}
