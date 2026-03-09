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

package infra.cloud.service;

import java.util.Collections;
import java.util.List;

/**
 * Metadata container for a service interface, holding the associated service metadata,
 * the service interface class, and an immutable list of its service methods.
 *
 * @param <M> the type of service method, which must extend {@link ServiceMethod}
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/9 21:59
 */
public class ServiceInterfaceMetadata<M extends ServiceMethod> {

  private final ServiceMetadata serviceMetadata;

  private final Class<?> serviceInterface;

  private final List<M> serviceMethods;

  /**
   * Constructs a new {@code ServiceInterfaceMetadata} instance.
   *
   * @param serviceInterface the class representing the service interface
   * @param serviceMetadata the metadata associated with the service
   * @param serviceMethods the list of service methods belonging to this interface
   */
  public ServiceInterfaceMetadata(Class<?> serviceInterface, ServiceMetadata serviceMetadata, List<M> serviceMethods) {
    this.serviceInterface = serviceInterface;
    this.serviceMetadata = serviceMetadata;
    this.serviceMethods = serviceMethods;
  }

  /**
   * Returns the class representing the service interface.
   *
   * @return the service interface class
   */
  public Class<?> getServiceInterface() {
    return serviceInterface;
  }

  /**
   * Returns the metadata associated with the service.
   *
   * @return the service metadata
   */
  public ServiceMetadata getServiceMetadata() {
    return serviceMetadata;
  }

  /**
   * Returns an unmodifiable list of service methods belonging to this interface.
   *
   * @return an immutable list of service methods
   */
  public List<M> getServiceMethods() {
    return Collections.unmodifiableList(serviceMethods);
  }

}
