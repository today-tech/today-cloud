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

package infra.cloud.registry;

import java.util.List;

import infra.cloud.DiscoveryClient;
import infra.cloud.ServiceInstance;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/11/19 21:19
 */
public class EtcdDiscoveryClient implements DiscoveryClient {

  @Override
  public List<ServiceInstance> getInstances(String serviceId) {
    return null;
  }

  @Override
  public List<String> getServices() {
    return null;
  }

}
