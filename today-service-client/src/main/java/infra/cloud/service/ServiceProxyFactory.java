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

package infra.cloud.service;

/**
 * Factory to create a client proxy from a REMOTE service interface
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 2021/7/4 22:58
 */
public interface ServiceProxyFactory extends ServiceProvider {

  /**
   * Return a proxy that implements the given service interface to perform
   * requests and retrieve responses through a client.
   *
   * @param service the service to create a proxy for
   * @param <S> the service type
   * @return the created proxy
   */
  @Override
  <S> S getService(Class<S> service);

}
