/*
 * Copyright 2021 - 2023 the original author or authors.
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

package cn.taketoday.cloud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import cn.taketoday.cloud.core.ServiceInstance;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;

/**
 * Represents read operations commonly available to discovery
 * services such as Netflix Eureka or consul.io.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0
 */
public interface DiscoveryClient {

  /**
   * Gets all ServiceInstances associated with a particular serviceId.
   *
   * @param serviceId The serviceId to query.
   * @return A List of ServiceInstance.
   */
  List<ServiceInstance> getInstances(String serviceId);

  /**
   * @return All known service IDs.
   */
  List<String> getServices();

  static Composite composite(List<DiscoveryClient> clients) {
    return new Composite(clients);
  }

  /**
   * A {@link DiscoveryClient} that is composed of other discovery clients and delegates
   * calls to each of them in order.
   *
   * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
   */
  class Composite implements DiscoveryClient {

    private final List<DiscoveryClient> clients;

    public Composite(List<DiscoveryClient> clients) {
      AnnotationAwareOrderComparator.sort(clients);
      this.clients = clients;
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceId) {
      if (this.clients != null) {
        for (DiscoveryClient discoveryClient : this.clients) {
          List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
          if (instances != null && !instances.isEmpty()) {
            return instances;
          }
        }
      }
      return Collections.emptyList();
    }

    @Override
    public List<String> getServices() {
      LinkedHashSet<String> services = new LinkedHashSet<>();
      if (this.clients != null) {
        for (DiscoveryClient discoveryClient : this.clients) {
          List<String> serviceForClient = discoveryClient.getServices();
          if (serviceForClient != null) {
            services.addAll(serviceForClient);
          }
        }
      }
      return new ArrayList<>(services);
    }

    public List<DiscoveryClient> getDiscoveryClients() {
      return this.clients;
    }

  }

}
