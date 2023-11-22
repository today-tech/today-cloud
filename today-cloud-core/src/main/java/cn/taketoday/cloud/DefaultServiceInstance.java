/*
 * Copyright 2021 - 2023 the original author or authors.
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

package cn.taketoday.cloud;

import java.net.URI;
import java.util.Objects;

import cn.taketoday.core.AttributeAccessorSupport;
import cn.taketoday.core.style.ToStringBuilder;

/**
 * Default implementation of {@link ServiceInstance}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/11/19 20:52
 */
public class DefaultServiceInstance extends AttributeAccessorSupport implements ServiceInstance {

  private String instanceId;

  private String serviceId;

  private String host;

  private int port;

  private URI uri;

  public DefaultServiceInstance() {

  }

  /**
   * @param instanceId the id of the instance.
   * @param serviceId the id of the service.
   * @param host the host where the service instance can be found.
   * @param port the port on which the service is running.
   */
  public DefaultServiceInstance(String instanceId, String serviceId, String host, int port) {
    this.instanceId = instanceId;
    this.serviceId = serviceId;
    this.host = host;
    this.port = port;
  }

  /**
   * Creates a URI from the given ServiceInstance's host:port.
   *
   * @param instance the ServiceInstance.
   * @return URI of the form "host:port". Scheme port default used
   * if port not set.
   */
  public static URI getUri(ServiceInstance instance) {
    int port = instance.getPort();
    if (port <= 0) {
      port = 80;
    }
    String uri = String.format("http://%s:%s", instance.getHost(), port);
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

  public void setUri(URI uri) {
    this.uri = uri;
    this.host = this.uri.getHost();
    this.port = this.uri.getPort();
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("instanceId", instanceId)
            .append("serviceId", serviceId)
            .append("host", host)
            .append("port", port)
            .append("uri", uri)
            .toString();
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (!(object instanceof DefaultServiceInstance that))
      return false;
    if (!super.equals(object))
      return false;
    return port == that.port
            && Objects.equals(instanceId, that.instanceId)
            && Objects.equals(serviceId, that.serviceId)
            && Objects.equals(host, that.host)
            && Objects.equals(uri, that.uri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), instanceId, serviceId, host, port, uri);
  }

}
