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

package infra.cloud.registry.simple;

import infra.cloud.net.InetService;
import infra.context.properties.ConfigurationProperties;
import infra.util.StringUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/23 21:52
 */
@ConfigurationProperties(prefix = "infra.cloud.service-registry.simple")
public class SimpleRegistryProperties {

  private boolean enabled = true;

  /** Id used to register with. Defaults to a random UUID. */
  private String instanceId;

  /**
   * Predefined host with which a service can register itself.
   */
  private String instanceHost;

  /**
   * IP address to use when accessing service (must also set preferIpAddress to use).
   */
  private String instanceIpAddress;

  /**
   * Use ip address rather than hostname during registration.
   */
  private boolean preferIpAddress = false;

  /** Port to register the service under (defaults to listening port). */
  private Integer instancePort;

  public SimpleRegistryProperties(InetService inetService) {
    var hostInfo = inetService.findFirstNonLoopbackHostInfo();
    this.instanceHost = hostInfo.getHostname();
    this.instanceIpAddress = hostInfo.getIpAddress();
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getInstanceHost() {
    if (this.preferIpAddress && StringUtils.hasText(this.instanceIpAddress)) {
      return this.instanceIpAddress;
    }
    return this.instanceHost;
  }

  public void setInstanceHost(String instanceHost) {
    this.instanceHost = instanceHost;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public void setInstanceId(String instanceId) {
    this.instanceId = instanceId;
  }

  public void setInstanceIpAddress(String instanceIpAddress) {
    this.instanceIpAddress = instanceIpAddress;
  }

  public Integer getInstancePort() {
    return instancePort;
  }

  public void setInstancePort(Integer instancePort) {
    this.instancePort = instancePort;
  }

  public boolean isPreferIpAddress() {
    return preferIpAddress;
  }

  public void setPreferIpAddress(boolean preferIpAddress) {
    this.preferIpAddress = preferIpAddress;
  }
}
