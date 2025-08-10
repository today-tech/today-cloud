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

package infra.cloud.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import infra.core.annotation.AnnotationAwareOrderComparator;
import infra.util.CollectionUtils;

/**
 * A {@link DiscoveryClient} that is composed of other discovery clients and delegates
 * calls to each of them in order.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/8 14:21
 */
public class CompositeDiscoveryClient implements DiscoveryClient {

  private final List<DiscoveryClient> discoveryClients;

  public CompositeDiscoveryClient(List<DiscoveryClient> discoveryClients) {
//    AnnotationAwareOrderComparator.sort(discoveryClients);
    this.discoveryClients = discoveryClients;
  }

  @Override
  public String getDescription() {
    return "Composite Discovery Client";
  }

  @Override
  public List<ServiceInstance> getInstances(String serviceId) {
    if (this.discoveryClients != null) {
      for (DiscoveryClient discoveryClient : this.discoveryClients) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
        if (CollectionUtils.isNotEmpty(instances)) {
          return instances;
        }
      }
    }
    return Collections.emptyList();
  }

  @Override
  public List<String> getServices() {
    LinkedHashSet<String> services = new LinkedHashSet<>();
    if (this.discoveryClients != null) {
      for (DiscoveryClient discoveryClient : this.discoveryClients) {
        List<String> serviceForClient = discoveryClient.getServices();
        if (serviceForClient != null) {
          services.addAll(serviceForClient);
        }
      }
    }
    return new ArrayList<>(services);
  }

  @Override
  public void probe() {
    if (this.discoveryClients != null) {
      for (DiscoveryClient discoveryClient : this.discoveryClients) {
        discoveryClient.probe();
      }
    }
  }

  public List<DiscoveryClient> getDiscoveryClients() {
    return discoveryClients;
  }

}
