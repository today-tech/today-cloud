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

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/6 22:16
 */
class InetServiceTests {

  @Test
  public void testGetFirstNonLoopbackHostInfo() {
    InetService inetService = new InetService(new InetProperties());
    then(inetService.findFirstNonLoopbackHostInfo()).isNotNull();
  }

  @Test
  public void testGetFirstNonLoopbackAddress() {
    InetService inetService = new InetService(new InetProperties());
    then(inetService.findFirstNonLoopbackAddress()).isNotNull();
  }

  @Test
  public void testConvert() throws Exception {
    InetService inetService = new InetService(new InetProperties());
    then(inetService.convertAddress(InetAddress.getByName("localhost"))).isNotNull();
  }

  @Test
  public void testHostInfo() {
    InetService inetService = new InetService(new InetProperties());
    HostInfo info = inetService.findFirstNonLoopbackHostInfo();
    then(info.getIpAddressAsInt()).isNotNull();
  }

  @Test
  public void testIgnoreInterface() {
    InetProperties properties = new InetProperties();
    properties.setIgnoredInterfaces(Arrays.asList("docker0", "veth.*"));
    InetService inetService = new InetService(properties);

    then(inetService.ignoreInterface("docker0")).isTrue().as("docker0 not ignored");
    then(inetService.ignoreInterface("vethAQI2QT")).as("vethAQI2QT0 not ignored").isTrue();
    then(inetService.ignoreInterface("docker1")).as("docker1 ignored").isFalse();
  }

  @Test
  public void testDefaultIgnoreInterface() {
    InetService inetService = new InetService(new InetProperties());
    then(inetService.ignoreInterface("docker0")).as("docker0 ignored").isFalse();
  }

  @Test
  public void testSiteLocalAddresses() throws Exception {
    InetProperties properties = new InetProperties();
    properties.setUseOnlySiteLocalInterfaces(true);

    InetService inetService = new InetService(properties);
    then(inetService.isPreferredAddress(InetAddress.getByName("192.168.0.1"))).isTrue();
    then(inetService.isPreferredAddress(InetAddress.getByName("5.5.8.1"))).isFalse();
  }

  @Test
  public void testPreferredNetworksRegex() throws Exception {
    InetProperties properties = new InetProperties();
    properties.setPreferredNetworks(Arrays.asList("192.168.*", "10.0.*"));

    InetService inetService = new InetService(properties);
    then(inetService.isPreferredAddress(InetAddress.getByName("192.168.0.1"))).isTrue();
    then(inetService.isPreferredAddress(InetAddress.getByName("5.5.8.1"))).isFalse();
    then(inetService.isPreferredAddress(InetAddress.getByName("10.0.10.1"))).isTrue();
    then(inetService.isPreferredAddress(InetAddress.getByName("10.255.10.1"))).isFalse();
  }

  @Test
  public void testPreferredNetworksSimple() throws Exception {
    InetProperties properties = new InetProperties();
    properties.setPreferredNetworks(Arrays.asList("192", "10.0"));

    InetService inetService = new InetService(properties);
    then(inetService.isPreferredAddress(InetAddress.getByName("192.168.0.1"))).isTrue();
    then(inetService.isPreferredAddress(InetAddress.getByName("5.5.8.1"))).isFalse();
    then(inetService.isPreferredAddress(InetAddress.getByName("10.255.10.1"))).isFalse();
    then(inetService.isPreferredAddress(InetAddress.getByName("10.0.10.1"))).isTrue();
  }

  @Test
  public void testPreferredNetworksListIsEmpty() throws Exception {
    InetProperties properties = new InetProperties();
    properties.setPreferredNetworks(Collections.emptyList());
    InetService inetService = new InetService(properties);
    then(inetService.isPreferredAddress(InetAddress.getByName("192.168.0.1"))).isTrue();
    then(inetService.isPreferredAddress(InetAddress.getByName("5.5.8.1"))).isTrue();
    then(inetService.isPreferredAddress(InetAddress.getByName("10.255.10.1"))).isTrue();
    then(inetService.isPreferredAddress(InetAddress.getByName("10.0.10.1"))).isTrue();
  }

}