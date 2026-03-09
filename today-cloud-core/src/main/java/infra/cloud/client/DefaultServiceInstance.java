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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import infra.core.style.ToStringBuilder;

/**
 * Default implementation of {@link ServiceInstance}.
 * <p>
 * This class represents a service instance with properties such as instance ID,
 * service ID, host, port, security status, and metadata. It provides constructors
 * for creating instances with varying levels of detail and includes standard
 * object methods like {@code toString()}, {@code equals()}, and {@code hashCode()}.
 * </p>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/11/19 20:52
 */
public class DefaultServiceInstance implements ServiceInstance {

  private String instanceId;

  private String serviceId;

  private String host;

  private int port;

  private boolean secure;

  private Map<String, String> metadata = new LinkedHashMap<>();

  public DefaultServiceInstance() {
  }

  /**
   * @param instanceId the id of the instance.
   * @param serviceId the id of the service.
   * @param host the host where the service instance can be found.
   * @param port the port on which the service is running.
   * @param secure indicates whether the connection needs to be secure.
   */
  public DefaultServiceInstance(String instanceId, String serviceId, String host, int port, boolean secure) {
    this(instanceId, serviceId, host, port, secure, new LinkedHashMap<>());
  }

  /**
   * @param instanceId the id of the instance.
   * @param serviceId the id of the service.
   * @param host the host where the service instance can be found.
   * @param port the port on which the service is running.
   * @param secure indicates whether the connection needs to be secure.
   * @param metadata a map containing metadata.
   */
  public DefaultServiceInstance(String instanceId, String serviceId, String host,
          int port, boolean secure, Map<String, String> metadata) {
    this.instanceId = instanceId;
    this.serviceId = serviceId;
    this.host = host;
    this.port = port;
    this.secure = secure;
    this.metadata = metadata;
  }

  @Override
  public Map<String, String> getMetadata() {
    return metadata;
  }

  @Override
  public String getInstanceId() {
    return instanceId;
  }

  @Override
  public String getServiceId() {
    return serviceId;
  }

  @Override
  public String getHost() {
    return host;
  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public boolean isSecure() {
    return secure;
  }

  public void setDefaultInstanceId() {
    setInstanceId(getHost() + ":" + getPort() + ":" + getServiceId());
  }

  public void setInstanceId(String instanceId) {
    this.instanceId = instanceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void setSecure(boolean secure) {
    this.secure = secure;
  }

  @Override
  public String toString() {
    return ToStringBuilder.forInstance(this)
            .append("instanceId", instanceId)
            .append("serviceId", serviceId)
            .append("host", host)
            .append("port", port)
            .append("secure", secure)
            .append("metadata", metadata)
            .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DefaultServiceInstance that = (DefaultServiceInstance) o;
    return port == that.port && secure == that.secure
            && Objects.equals(instanceId, that.instanceId)
            && Objects.equals(serviceId, that.serviceId)
            && Objects.equals(host, that.host)
            && Objects.equals(metadata, that.metadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(instanceId, serviceId, host, port, secure, metadata);
  }

}
