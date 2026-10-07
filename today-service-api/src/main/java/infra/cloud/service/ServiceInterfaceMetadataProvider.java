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

/**
 * Provider interface for retrieving metadata associated with a service interface.
 * <p>
 * This interface is responsible for extracting and providing structural or descriptive
 * information about a given service interface, typically used in RPC or cloud service
 * discovery scenarios. The metadata is generic over {@link ServiceMethod} to allow
 * flexible method-level descriptions.
 *
 * @param <M> the type of service method metadata, which must extend {@link ServiceMethod}
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/10 08:22
 */
public interface ServiceInterfaceMetadataProvider<M extends ServiceMethod> {

  /**
   * Retrieves the metadata for the specified service interface.
   *
   * @param serviceInterface the class object of the service interface to inspect
   * @return the metadata associated with the given service interface
   */
  ServiceInterfaceMetadata<M> getMetadata(Class<?> serviceInterface);

}
