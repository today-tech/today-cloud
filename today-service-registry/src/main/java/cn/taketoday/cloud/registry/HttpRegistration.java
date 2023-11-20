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

import java.util.List;

import cn.taketoday.cloud.Registration;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/11/20 21:59
 */
public class HttpRegistration implements Registration {

  private List<ServiceDefinition> serviceDefinitions;

  public HttpRegistration() { }

  public HttpRegistration(List<ServiceDefinition> serviceDefinitions) {
    this.serviceDefinitions = serviceDefinitions;
  }

  public void setServiceDefinitions(List<ServiceDefinition> serviceDefinitions) {
    this.serviceDefinitions = serviceDefinitions;
  }

  public List<ServiceDefinition> getServiceDefinitions() {
    return serviceDefinitions;
  }

}
