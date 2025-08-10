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

import java.util.List;

import infra.core.Ordered;
import infra.lang.Descriptive;

/**
 * Represents read operations commonly available to discovery
 * services such as Netflix Eureka or consul.io.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0
 */
public interface DiscoveryClient extends Descriptive, Ordered {

  /**
   * Default order of the discovery client.
   */
  int DEFAULT_ORDER = 0;

  /**
   * A human-readable description of the implementation.
   *
   * @return The description.
   */
  @Override
  String getDescription();

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

  /**
   * Can be used to verify the client is valid and able to make calls.
   * <p>
   * A successful invocation with no exception thrown implies the client is able to make
   * calls.
   * <p>
   * The default implementation simply calls {@link #getServices()} - client
   * implementations can override with a lighter weight operation if they choose to.
   */
  default void probe() {
    getServices();
  }

  /**
   * Default implementation for getting order of discovery clients.
   *
   * @return order
   */
  @Override
  default int getOrder() {
    return DEFAULT_ORDER;
  }

}
