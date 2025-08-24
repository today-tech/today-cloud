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
