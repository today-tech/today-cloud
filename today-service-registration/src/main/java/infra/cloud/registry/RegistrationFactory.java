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
import infra.cloud.service.ServiceMetadata;

/**
 * A factory interface responsible for creating {@link Registration} instances.
 * <p>
 * Implementations of this interface should generate a specific registration object
 * based on the provided {@link ServiceMetadata}. This is typically used during the
 * service discovery process to register a service instance with a registry center.
 *
 * @param <R> the type of registration, which must extend {@link Registration}
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/24 22:12
 */
public interface RegistrationFactory<R extends Registration> {

  /**
   * Creates a new registration instance using the given service metadata.
   *
   * @param serviceMetadata the metadata containing information about the service to be registered
   * @return a new instance of {@code R} representing the service registration
   */
  R createRegistration(ServiceMetadata serviceMetadata);

}
