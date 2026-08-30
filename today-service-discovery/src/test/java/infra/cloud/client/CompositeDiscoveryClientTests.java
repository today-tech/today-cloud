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
