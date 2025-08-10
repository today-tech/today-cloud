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

package infra.cloud.net;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import infra.beans.factory.annotation.Value;
import infra.context.properties.ConfigurationProperties;

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
  public static final String PREFIX = "cloud.inet";

  /**
   * The default hostname. Used in case of errors.
   */
  private String defaultHostname = "localhost";

  /**
   * The default IP address. Used in case of errors.
   */
  private String defaultIpAddress = "127.0.0.1";

  /**
   * Timeout, in seconds, for calculating hostname.
   */
  @Value("${cloud.inet.timeout.sec:${CLOUD_INET_TIMEOUT_SEC:4}}")
  private int timeoutSeconds = 1;

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

  public int getTimeoutSeconds() {
    return this.timeoutSeconds;
  }

  public void setTimeoutSeconds(int timeoutSeconds) {
    this.timeoutSeconds = timeoutSeconds;
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
