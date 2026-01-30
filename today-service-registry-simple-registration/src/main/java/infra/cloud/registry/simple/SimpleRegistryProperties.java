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

  /** ID used to register with. Defaults to a random UUID. */
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

  public boolean isPreferIpAddress() {
    return preferIpAddress;
  }

  public void setPreferIpAddress(boolean preferIpAddress) {
    this.preferIpAddress = preferIpAddress;
  }
}
