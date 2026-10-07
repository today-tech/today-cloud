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

package infra.cloud.net;

import java.net.InetAddress;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import infra.context.properties.ConfigurationProperties;
import infra.format.annotation.DurationUnit;

/**
 * Properties for {@link InetService}.
 *
 * @author Spencer Gibb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
@ConfigurationProperties(InetProperties.PREFIX)
public class InetProperties {

  /**
   * Prefix for the Inet properties.
   */
  public static final String PREFIX = "infra.cloud.inet";

  /**
   * The default hostname. Used in case of errors.
   */
  private String defaultHostname = "localhost";

  /**
   * The default IP address. Used in case of errors.
   */
  private String defaultIpAddress = "127.0.0.1";

  /**
   * Timeout for calculating hostname.
   */
  @DurationUnit(ChronoUnit.SECONDS)
  private Duration timeout = Duration.ofSeconds(4);

  /**
   * List of Java regular expressions for network interfaces that will be ignored.
   */
  private List<String> ignoredInterfaces = new ArrayList<>();

  /**
   * Whether to use only interfaces with site local addresses. See
   * {@link InetAddress#isSiteLocalAddress()} for more details.
   */
  private boolean useOnlySiteLocalInterfaces = false;

  /**
   * List of Java regular expressions for network addresses that will be preferred.
   */
  private List<String> preferredNetworks = new ArrayList<>();

  public String getDefaultHostname() {
    return this.defaultHostname;
  }

  public void setDefaultHostname(String defaultHostname) {
    this.defaultHostname = defaultHostname;
  }

  public String getDefaultIpAddress() {
    return this.defaultIpAddress;
  }

  public void setDefaultIpAddress(String defaultIpAddress) {
    this.defaultIpAddress = defaultIpAddress;
  }

  public Duration getTimeout() {
    return timeout;
  }

  public void setTimeout(Duration timeout) {
    this.timeout = timeout;
  }

  public List<String> getIgnoredInterfaces() {
    return this.ignoredInterfaces;
  }

  public void setIgnoredInterfaces(List<String> ignoredInterfaces) {
    this.ignoredInterfaces = ignoredInterfaces;
  }

  public boolean isUseOnlySiteLocalInterfaces() {
    return this.useOnlySiteLocalInterfaces;
  }

  public void setUseOnlySiteLocalInterfaces(boolean useOnlySiteLocalInterfaces) {
    this.useOnlySiteLocalInterfaces = useOnlySiteLocalInterfaces;
  }

  public List<String> getPreferredNetworks() {
    return this.preferredNetworks;
  }

  public void setPreferredNetworks(List<String> preferredNetworks) {
    this.preferredNetworks = preferredNetworks;
  }

}
