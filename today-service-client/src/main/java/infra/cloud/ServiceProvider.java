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

package infra.cloud;

import infra.cloud.registry.ServiceNotFoundException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/8/14 21:53
 */
public interface ServiceProvider {

  /**
   * Lookup a service
   *
   * @param serviceInterface service interface type
   * @param <T> Service type
   * @return service
   * @throws ServiceNotFoundException service not found
   */
  <T> T getService(Class<T> serviceInterface);

}
