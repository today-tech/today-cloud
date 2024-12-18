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

package cn.taketoday.config.etcd;

import infra.beans.factory.annotation.Value;
import infra.context.properties.ConfigurationProperties;
import infra.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/11/7 21:51
 */
@ConfigurationProperties("config.server.etcd")
public class EtcdProperties {

  /**
   * Etcd server endpoints.
   */
  private String[] endpoints;

  /**
   * Etcd server key namespace.
   */
  @Nullable
  @Value("${app.name:application}")
  private String namespace;

  /**
   * Etcd server user name.
   */
  private String username;

  /**
   * Etcd server user password.
   */
  private String password;

  public void setNamespace(@Nullable String namespace) {
    this.namespace = namespace;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setEndpoints(String[] endpoints) {
    this.endpoints = endpoints;
  }

  public String[] getEndpoints() {
    return endpoints;
  }

  @Nullable
  public String getNamespace() {
    return namespace;
  }

  public String getPassword() {
    return password;
  }

  public String getUsername() {
    return username;
  }
}
