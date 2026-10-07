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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import infra.cloud.client.simple.SimpleDiscoveryClient;
import infra.cloud.client.simple.SimpleDiscoveryProperties;

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