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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import infra.core.style.ToStringBuilder;
import infra.lang.Nullable;

/**
 * Default implementation of {@link ServiceInstance}.
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

  @Nullable
  private URI uri;

  public DefaultServiceInstance() {
  }

  /**
   * @param instanceId the id of the instance.
   * @param serviceId the id of the service.
   * @param host the host where the service instance can be found.
   * @param port the port on which the service is running.
   * @param secure indicates whether or not the connection needs to be secure.
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

  /**
   * @param instanceId the id of the instance.
   * @param serviceId the id of the service.
   * @param host the host where the service instance can be found.
   * @param port the port on which the service is running.
   * @param secure indicates whether or not the connection needs to be secure.
   */
  public DefaultServiceInstance(String instanceId, String serviceId, String host, int port, boolean secure) {
    this(instanceId, serviceId, host, port, secure, new LinkedHashMap<>());
  }

  /**
   * Creates a URI from the given ServiceInstance's host:port.
   *
   * @param instance the ServiceInstance.
   * @return URI of the form (secure)?https:http + "host:port". Scheme port default used
   * if port not set.
   */
  public static URI getUri(ServiceInstance instance) {
    String scheme = (instance.isSecure()) ? "https" : "http";
    int port = instance.getPort();
    if (port <= 0) {
      port = (instance.isSecure()) ? 443 : 80;
    }
    String uri = String.format("%s://%s:%s", scheme, instance.getHost(), port);
    return URI.create(uri);
  }

  @Override
  public URI getHttpURI() {
    if (uri == null) {
      uri = getUri(this);
    }
    return uri;
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

  public void setUri(URI uri) {
    this.uri = uri;
    this.host = this.uri.getHost();
    this.port = this.uri.getPort();
    String scheme = this.uri.getScheme();
    if ("https".equals(scheme)) {
      this.secure = true;
    }
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
