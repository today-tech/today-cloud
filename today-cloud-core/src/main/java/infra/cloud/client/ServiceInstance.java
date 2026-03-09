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

package infra.cloud.client;

import java.util.Map;

/**
 * Represents an instance of a service in the cloud infrastructure.
 * This interface defines the contract for accessing information about
 * a specific service instance including its identity, location, and metadata.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/11/19 20:52
 */
public interface ServiceInstance {

  /**
   * @return The unique instance ID as registered.
   */
  default String getInstanceId() {
    return getHost() + ":" + getPort() + ":" + getServiceId();
  }

  /**
   * @return The service ID as registered.
   */
  String getServiceId();

  /**
   * @return The hostname of the registered service instance.
   */
  String getHost();

  /**
   * @return The port of the registered service instance.
   */
  int getPort();

  /**
   * @return Whether the port of the registered service instance uses HTTPS.
   */
  boolean isSecure();

  /**
   * @return The key / value pair metadata associated with the service instance.
   */
  Map<String, String> getMetadata();

}
