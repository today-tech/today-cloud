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

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
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
    HostInfo hostInfo = new HostInfo();
    hostInfo.setHostname(this.properties.getDefaultHostname());
    hostInfo.setIpAddress(this.properties.getDefaultIpAddress());
    return hostInfo;
  }

  public InetAddress findFirstNonLoopbackAddress() {
    InetAddress result = null;
    try {
      int lowest = Integer.MAX_VALUE;
      for (Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces(); nics
              .hasMoreElements(); ) {
        NetworkInterface ifc = nics.nextElement();
        if (ifc.isUp()) {
          log.trace("Testing interface: {}", ifc.getDisplayName());
          if (ifc.getIndex() < lowest || result == null) {
            lowest = ifc.getIndex();
          }
          else if (result != null) {
            continue;
          }

          // @formatter:off
					if (!ignoreInterface(ifc.getDisplayName())) {
						for (Enumeration<InetAddress> addrs = ifc
								.getInetAddresses(); addrs.hasMoreElements();) {
							InetAddress address = addrs.nextElement();
							if (address instanceof Inet4Address
									&& !address.isLoopbackAddress()
									&& isPreferredAddress(address)) {
								log.trace("Found non-loopback interface: {}" , ifc.getDisplayName());
								result = address;
							}
						}
					}
					// @formatter:on
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

    if (this.properties.isUseOnlySiteLocalInterfaces()) {
      final boolean siteLocalAddress = address.isSiteLocalAddress();
      if (!siteLocalAddress) {
        log.trace("Ignoring address: {}", address.getHostAddress());
      }
      return siteLocalAddress;
    }
    final List<String> preferredNetworks = this.properties.getPreferredNetworks();
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
    for (String regex : this.properties.getIgnoredInterfaces()) {
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
      hostname = result.get(properties.getTimeoutSeconds(), TimeUnit.SECONDS);
    }
    catch (Exception e) {
      result.cancel(true);
      log.info("Cannot determine local hostname");
      hostname = "localhost";
    }
    return new HostInfo(hostname, address.getHostAddress());
  }

}
