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

package cn.taketoday.cloud.registry;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.util.Objects;

import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.lang.Constant;
import cn.taketoday.web.util.UriComponentsBuilder;

/**
 * @author TODAY 2021/7/4 00:36
 */
public class ServiceDefinition implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private int port;
  private String host;
  private String name; // service name

  private String version;

  @JsonIgnore
  private transient Class<?> serviceInterface;

  private transient URI httpURI;

  public void setVersion(String version) {
    this.version = version;
  }

  public String getVersion() {
    return version;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getName() {
    return name;
  }

  public int getPort() {
    return port;
  }

  public String getHost() {
    return host;
  }

  @JsonIgnore
  public Class<?> getServiceInterface() {
    return serviceInterface;
  }

  @JsonIgnore
  public void setServiceInterface(Class<?> serviceInterface) {
    this.serviceInterface = serviceInterface;
  }

  public URI getHttpURI() {
    URI httpURI = this.httpURI;
    if (httpURI == null) {
      httpURI = UriComponentsBuilder.newInstance().scheme(Constant.HTTP).host(host).port(port).build().toUri();
      this.httpURI = httpURI;
    }
    return httpURI;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof ServiceDefinition that))
      return false;
    return port == that.port
            && Objects.equals(host, that.host)
            && Objects.equals(name, that.name)
            && Objects.equals(version, that.version)
            && Objects.equals(serviceInterface, that.serviceInterface);
  }

  @Override
  public int hashCode() {
    return Objects.hash(port, host, name, version, serviceInterface);
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("port", port)
            .append("host", host)
            .append("name", name)
            .append("version", version)
            .append("serviceInterface", serviceInterface)
            .toString();
  }

}
