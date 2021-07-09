/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.rpc.registry;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author TODAY 2021/7/4 00:36
 */
public class ServiceDefinition implements Serializable {
  private static final long serialVersionUID = 1L;

  private int port;
  private String host;
  private String name; // service name

  private String version;

  private transient Class<?> serviceInterface;

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

  public Class<?> getServiceInterface() {
    return serviceInterface;
  }

  public void setServiceInterface(Class<?> serviceInterface) {
    this.serviceInterface = serviceInterface;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof ServiceDefinition))
      return false;
    final ServiceDefinition that = (ServiceDefinition) o;
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
}
