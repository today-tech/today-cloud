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

package infra.cloud.registry;

import infra.context.properties.ConfigurationProperties;

/**
 * @author Spencer Gibb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
@ConfigurationProperties("infra.cloud.service-registry.auto-registration")
public class AutoServiceRegistrationProperties {

  /**
   * Whether service auto-registration is enabled. Defaults to true.
   */
  private boolean enabled = true;

  /**
   * Whether startup fails if there is no AutoServiceRegistration. Defaults to false.
   */
  private boolean failFast = false;

  /**
   * Service provider port
   */
  private int port = 9000;

  public boolean isEnabled() {
    return this.enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isFailFast() {
    return this.failFast;
  }

  public void setFailFast(boolean failFast) {
    this.failFast = failFast;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public int getPort() {
    return port;
  }
}
