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
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * Host information.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/6 23:07
 */
public class HostInfo {

  /**
   * Should override the host info.
   */
  public boolean override;

  private String ipAddress;

  private String hostname;

  public HostInfo(String hostname, String ipAddress) {
    this.hostname = hostname;
    this.ipAddress = ipAddress;
  }

  public HostInfo() {
  }

  public int getIpAddressAsInt() {
    InetAddress inetAddress;
    String host = this.ipAddress;
    if (host == null) {
      host = this.hostname;
    }
    try {
      inetAddress = InetAddress.getByName(host);
    }
    catch (final UnknownHostException e) {
      throw new IllegalArgumentException(e);
    }
    return ByteBuffer.wrap(inetAddress.getAddress()).getInt();
  }

  public boolean isOverride() {
    return this.override;
  }

  public void setOverride(boolean override) {
    this.override = override;
  }

  public String getIpAddress() {
    return this.ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public String getHostname() {
    return this.hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

}
