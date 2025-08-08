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

/**
 * Contract to register and deregister instances with a Service Registry.
 *
 * @param <R> registration meta data
 * @param <S> The type of the status.
 * @author Spencer Gibb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 2021/7/4 23:12
 */
public interface ServiceRegistry<R extends Registration, S> {

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

  /**
   * Sets the status of the registration. The status values are determined by the
   * individual implementations.
   *
   * @param registration The registration to update.
   * @param status The status to set.
   */
  void setStatus(R registration, S status);

  /**
   * Gets the status of a particular registration.
   *
   * @param registration The registration to query.
   * @return The status of the registration.
   */
  S getStatus(R registration);

  /**
   * Closes the ServiceRegistry. This is a lifecycle method.
   */
  void close();

}
