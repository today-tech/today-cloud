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

import java.util.List;

import infra.app.test.context.InfraTest;
import infra.beans.factory.annotation.Autowired;

import static infra.cloud.client.CompositeDiscoveryClientTestsConfig.CUSTOM_DISCOVERY_CLIENT;
import static infra.cloud.client.CompositeDiscoveryClientTestsConfig.CUSTOM_SERVICE_ID;
import static infra.cloud.client.CompositeDiscoveryClientTestsConfig.DEFAULT_ORDER_DISCOVERY_CLIENT;
import static infra.cloud.client.CompositeDiscoveryClientTestsConfig.FOURTH_DISCOVERY_CLIENT;
import static org.assertj.core.api.BDDAssertions.then;

/**
 * Tests for the support of ordered {@link DiscoveryClient} instances in
 * {@link CompositeDiscoveryClient}.
 *
 * @author Olga Maciaszek-Sharma
 */
@InfraTest(properties = "infra.cloud.discovery.simple.order=2",
        classes = CompositeDiscoveryClientTestsConfig.class)
class CompositeDiscoveryClientOrderTest {

  @Autowired
  CompositeDiscoveryClient discoveryClient;

  @Test
  void shouldGetOrderedDiscoveryClients() {
    List<DiscoveryClient> discoveryClients = this.discoveryClient.getDiscoveryClients();

    then(discoveryClients.get(0).getDescription()).isEqualTo(CUSTOM_DISCOVERY_CLIENT);
    then(discoveryClients.get(1).getDescription()).isEqualTo(DEFAULT_ORDER_DISCOVERY_CLIENT);
    then(discoveryClients.get(2).getDescription()).isEqualTo("Simple Discovery Client");
    then(discoveryClients.get(3).getDescription()).isEqualTo(FOURTH_DISCOVERY_CLIENT);
  }

  @Test
  void shouldOnlyReturnServiceInstancesForTheHighestPrecedenceDiscoveryClient() {
    List<ServiceInstance> serviceInstances = this.discoveryClient.getInstances(CUSTOM_SERVICE_ID);

    then(serviceInstances).hasSize(1);
    then(serviceInstances.get(0).getPort()).isEqualTo(123);
  }

}
