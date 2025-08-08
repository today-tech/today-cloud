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

package infra.cloud.client;

import org.junit.jupiter.api.Test;

import infra.app.test.context.InfraTest;
import infra.beans.factory.annotation.Autowired;

import static infra.cloud.client.CompositeDiscoveryClientTestsConfig.CUSTOM_SERVICE_ID;
import static org.assertj.core.api.BDDAssertions.then;

/**
 * Tests for behavior of Composite Discovery Client.
 *
 * @author Biju Kunjummen
 */
@InfraTest(properties = { "app.name=service0",
        "infra.cloud.discovery.simple.instances.service1[0].uri=http://s11:8080",
        "infra.cloud.discovery.simple.instances.service1[1].uri=https://s12:8443",
        "infra.cloud.discovery.simple.instances.service2[0].uri=https://s21:8080",
        "infra.cloud.discovery.simple.instances.service2[1].uri=https://s22:443" },
        classes = CompositeDiscoveryClientTestsConfig.class)
class CompositeDiscoveryClientTests {

  @Autowired
  private DiscoveryClient discoveryClient;

  @Test
  void getInstancesByServiceIdShouldDelegateCall() {
    then(this.discoveryClient).isInstanceOf(CompositeDiscoveryClient.class);

    then(this.discoveryClient.getInstances("service1")).hasSize(2);

    ServiceInstance s1 = this.discoveryClient.getInstances("service1").get(0);
    then(s1.getHost()).isEqualTo("s11");
    then(s1.getPort()).isEqualTo(8080);
    then(s1.isSecure()).isEqualTo(false);
  }

  @Test
  void getServicesShouldAggregateAllServiceNames() {
    then(this.discoveryClient.getServices()).containsOnlyOnce("service1", "service2", "custom");
  }

  @Test
  void getDescriptionShouldBeComposite() {
    then(this.discoveryClient.getDescription()).isEqualTo("Composite Discovery Client");
  }

  @Test
  void getInstancesShouldRespectOrder() {
    then(this.discoveryClient.getInstances(CUSTOM_SERVICE_ID)).hasSize(1);
    then(this.discoveryClient.getInstances(CUSTOM_SERVICE_ID)).hasSize(1);
  }

  @Test
  void getInstancesByUnknownServiceIdShouldReturnAnEmptyList() {
    then(this.discoveryClient.getInstances("unknown")).hasSize(0);
  }

}
