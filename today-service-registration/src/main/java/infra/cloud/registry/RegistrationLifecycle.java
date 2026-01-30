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
 * Service registration life cycle. This life cycle is only related to
 * {@link Registration}.
 *
 * @author Zen Huifer
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
public interface RegistrationLifecycle<R extends Registration> {

  /**
   * A method executed before registering the local service with the
   * {@link ServiceRegistry}.
   *
   * @param registration registration
   */
  void postProcessBeforeStartRegister(R registration);

  /**
   * A method executed after registering the local service with the
   * {@link ServiceRegistry}.
   *
   * @param registration registration
   */
  void postProcessAfterStartRegister(R registration);

  /**
   * A method executed before de-registering the local service with the
   * {@link ServiceRegistry}.
   *
   * @param registration registration
   */
  void postProcessBeforeStopRegister(R registration);

  /**
   * A method executed after de-registering the local service with the
   * {@link ServiceRegistry}.
   *
   * @param registration registration
   */
  void postProcessAfterStopRegister(R registration);

}
