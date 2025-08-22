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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import infra.cloud.client.DiscoveryClient;
import infra.cloud.client.ServiceInstance;

/**
 * A {@link infra.cloud.client.DiscoveryClient} that will use the
 * properties file as a source of service instances.
 *
 * @author Biju Kunjummen
 * @author Olga Maciaszek-Sharma
 * @author Charu Covindane
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
public class SimpleDiscoveryClient implements DiscoveryClient {

  private final SimpleDiscoveryProperties properties;

  public SimpleDiscoveryClient(SimpleDiscoveryProperties properties) {
    this.properties = properties;
  }

  @Override
  public String getDescription() {
    return "Simple Discovery Client";
  }

  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public List<ServiceInstance> getInstances(String serviceId) {
    List instances = properties.getInstances().get(serviceId);
    if (instances != null) {
      return Collections.unmodifiableList(instances);
    }
    return Collections.emptyList();
  }

  @Override
  public List<String> getServices() {
    return new ArrayList<>(properties.getInstances().keySet());
  }

  @Override
  public int getOrder() {
    return properties.getOrder();
  }

}
