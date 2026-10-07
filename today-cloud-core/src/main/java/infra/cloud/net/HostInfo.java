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
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import infra.util.Assert;

/**
 * Host information.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/6 23:07
 */
public class HostInfo {

  private final String hostname;

  private final String ipAddress;

  public HostInfo(String hostname, String ipAddress) {
    Assert.notNull(hostname, "hostname is required");
    Assert.notNull(ipAddress, "ipAddress is required");
    this.hostname = hostname;
    this.ipAddress = ipAddress;
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

  public String getIpAddress() {
    return this.ipAddress;
  }

  public String getHostname() {
    return this.hostname;
  }

}
