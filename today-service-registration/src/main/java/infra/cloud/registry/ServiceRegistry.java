/*
 * Copyright 2021 - 2026 the TODAY authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.cloud.registry;

import infra.cloud.client.Registration;

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
