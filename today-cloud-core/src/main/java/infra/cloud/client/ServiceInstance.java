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

package infra.cloud.client;

import java.net.URI;
import java.util.Map;

/**
 * Represents an instance of a service.
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
   * @return The service URI address.
   */
  URI getHttpURI();

  /**
   * @return The key / value pair metadata associated with the service instance.
   */
  Map<String, String> getMetadata();

}
