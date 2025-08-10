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

package infra.cloud.client.simple;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import infra.cloud.client.DefaultServiceInstance;
import infra.cloud.client.ServiceInstance;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/8 20:58
 */
class SimpleDiscoveryClientTests {

  private SimpleDiscoveryClient simpleDiscoveryClient;

  @BeforeEach
  void setUp() {
    SimpleDiscoveryProperties simpleDiscoveryProperties = new SimpleDiscoveryProperties();

    Map<String, List<DefaultServiceInstance>> map = new HashMap<>();
    DefaultServiceInstance service1Inst1 = new DefaultServiceInstance(null, null, "host1", 8080, false);
    DefaultServiceInstance service1Inst2 = new DefaultServiceInstance(null, null, "host2", 0, true);
    DefaultServiceInstance service1Inst3 = new DefaultServiceInstance(null, null, "host3", 0, false);
    map.put("service1", Arrays.asList(service1Inst1, service1Inst2, service1Inst3));
    simpleDiscoveryProperties.setInstances(map);
    simpleDiscoveryProperties.afterPropertiesSet();
    this.simpleDiscoveryClient = new SimpleDiscoveryClient(simpleDiscoveryProperties);
  }

  @Test
  void shouldBeAbleToRetrieveServiceDetailsByName() {
    List<ServiceInstance> instances = this.simpleDiscoveryClient.getInstances("service1");
    then(instances.size()).isEqualTo(3);
    then(instances.get(0).getServiceId()).isEqualTo("service1");
    then(instances.get(0).getHost()).isEqualTo("host1");
    then(instances.get(0).getPort()).isEqualTo(8080);
    then(instances.get(0).isSecure()).isEqualTo(false);
    then(instances.get(0).getMetadata()).isNotNull();

    then(instances.get(1).getServiceId()).isEqualTo("service1");
    then(instances.get(1).getHost()).isEqualTo("host2");
    then(instances.get(1).getPort()).isEqualTo(0);
    then(instances.get(1).isSecure()).isEqualTo(true);
    then(instances.get(1).getMetadata()).isNotNull();

    then(instances.get(2).getServiceId()).isEqualTo("service1");
    then(instances.get(2).getHost()).isEqualTo("host3");
    then(instances.get(2).getPort()).isEqualTo(0);
    then(instances.get(2).isSecure()).isEqualTo(false);
    then(instances.get(2).getMetadata()).isNotNull();
  }

}