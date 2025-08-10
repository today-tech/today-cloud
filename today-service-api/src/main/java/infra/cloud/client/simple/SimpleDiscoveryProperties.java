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
