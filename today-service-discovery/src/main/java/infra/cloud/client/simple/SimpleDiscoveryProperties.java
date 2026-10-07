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

package infra.cloud.client.simple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import infra.beans.factory.InitializingBean;
import infra.cloud.client.DefaultServiceInstance;
import infra.context.properties.ConfigurationProperties;
import infra.core.OrderedSupport;

/**
 * Properties to hold the details of a {@link infra.cloud.client.DiscoveryClient}
 * service instances for a given service. It also holds the user-configurable order
 * that will be used to establish the precedence of this client in the list of clients
 * used by {@link infra.cloud.client.CompositeDiscoveryClient}.
 *
 * @author Biju Kunjummen
 * @author Olga Maciaszek-Sharma
 * @author Tim Ysewyn
 * @author Charu Covindane
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
@ConfigurationProperties(prefix = "infra.cloud.discovery.simple")
public class SimpleDiscoveryProperties extends OrderedSupport implements InitializingBean {

  private Map<String, List<DefaultServiceInstance>> instances = new HashMap<>();

  public Map<String, List<DefaultServiceInstance>> getInstances() {
    return this.instances;
  }

  public void setInstances(Map<String, List<DefaultServiceInstance>> instances) {
    this.instances = instances;
  }

  @Override
  public void afterPropertiesSet() {
    for (String key : this.instances.keySet()) {
      for (DefaultServiceInstance instance : this.instances.get(key)) {
        instance.setServiceId(key);
      }
    }
  }

}
