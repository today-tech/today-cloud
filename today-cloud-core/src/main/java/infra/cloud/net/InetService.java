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

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.concurrent.Future;

import static infra.util.concurrent.Future.run;

/**
 * @author Spencer Gibb
 * @author Sergey Tsypanov
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
public class InetService {

  private static final Logger log = LoggerFactory.getLogger(InetService.class);

  private final InetProperties properties;

  public InetService(InetProperties properties) {
    this.properties = properties;
  }

  public HostInfo findFirstNonLoopbackHostInfo() {
    InetAddress address = findFirstNonLoopbackAddress();
    if (address != null) {
      return convertAddress(address);
    }
    return new HostInfo(properties.getDefaultHostname(), properties.getDefaultIpAddress());
  }

  public InetAddress findFirstNonLoopbackAddress() {
    InetAddress result = null;
    try {
      int lowest = Integer.MAX_VALUE;
      var networkInterfaces = NetworkInterface.getNetworkInterfaces();
      while (networkInterfaces.hasMoreElements()) {
        NetworkInterface ifc = networkInterfaces.nextElement();
        if (ifc.isUp()) {
          log.trace("Testing interface: {}", ifc.getDisplayName());
          if (ifc.getIndex() < lowest || result == null) {
            lowest = ifc.getIndex();
          }
          else if (result != null) {
            continue;
          }

          if (!ignoreInterface(ifc.getDisplayName())) {
            var addresses = ifc.getInetAddresses();
            while (addresses.hasMoreElements()) {
              InetAddress address = addresses.nextElement();
              if (address instanceof Inet4Address && !address.isLoopbackAddress() && isPreferredAddress(address)) {
                log.trace("Found non-loopback interface: {}", ifc.getDisplayName());
                result = address;
              }
            }
          }
        }
      }
    }
    catch (IOException ex) {
      log.error("Cannot get first non-loopback address", ex);
    }

    if (result != null) {
      return result;
    }

    try {
      return InetAddress.getLocalHost();
    }
    catch (UnknownHostException e) {
      log.warn("Unable to retrieve localhost");
    }

    return null;
  }

  // For testing.
  boolean isPreferredAddress(InetAddress address) {
    if (properties.isUseOnlySiteLocalInterfaces()) {
      final boolean siteLocalAddress = address.isSiteLocalAddress();
      if (!siteLocalAddress) {
        log.trace("Ignoring address: {}", address.getHostAddress());
      }
      return siteLocalAddress;
    }
    final List<String> preferredNetworks = properties.getPreferredNetworks();
    if (preferredNetworks.isEmpty()) {
      return true;
    }
    for (String regex : preferredNetworks) {
      final String hostAddress = address.getHostAddress();
      if (hostAddress.matches(regex) || hostAddress.startsWith(regex)) {
        return true;
      }
    }
    log.trace("Ignoring address: {}", address.getHostAddress());
    return false;
  }

  // For testing
  boolean ignoreInterface(String interfaceName) {
    for (String regex : properties.getIgnoredInterfaces()) {
      if (interfaceName.matches(regex)) {
        log.trace("Ignoring interface: {}", interfaceName);
        return true;
      }
    }
    return false;
  }

  public HostInfo convertAddress(final InetAddress address) {
    Future<String> result = run(address::getHostName);

    String hostname;
    try {
      hostname = result.get(properties.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
    }
    catch (Exception e) {
      result.cancel(true);
      log.info("Cannot determine local hostname");
      hostname = "localhost";
    }
    return new HostInfo(hostname, address.getHostAddress());
  }

}
