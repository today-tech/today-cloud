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

import infra.cloud.Registration;

/**
 * @author TODAY 2021/7/4 23:12
 */
public interface ServiceRegistry<R extends Registration> {

  /**
   * Registers the registration. A registration typically has information about an
   * instance, such as its hostname and port.
   *
   * @param registration registration meta data
   * @throws ServiceRegisterFailedException If register failed
   */
  void register(R registration);

  /**
   * unregister the registration.
   *
   * @param registration registration meta data
   */
  void unregister(R registration);

}
