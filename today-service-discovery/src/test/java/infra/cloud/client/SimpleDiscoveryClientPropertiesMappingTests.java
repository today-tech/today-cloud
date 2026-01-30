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
import infra.cloud.client.simple.SimpleDiscoveryClient;
import infra.cloud.client.simple.SimpleDiscoveryProperties;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.EnableAutoConfiguration;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Tests for mapping properties to instances in {@link SimpleDiscoveryClient}.
 *
 * @author Biju Kunjummen
 */
@InfraTest(properties = { "app.name=service0",
        "infra.cloud.discovery.simple.instances.service1[0].uri=http://s11:8080",
        "infra.cloud.discovery.simple.instances.service1[1].uri=https://s12:8443",
        "infra.cloud.discovery.simple.instances.service2[0].uri=https://s21:8080",
        "infra.cloud.discovery.simple.instances.service2[1].uri=https://s22:443" })
class SimpleDiscoveryClientPropertiesMappingTests {

  @Autowired
  private SimpleDiscoveryProperties props;

  @Autowired
  private SimpleDiscoveryClient discoveryClient;

  @Test
  void propsShouldGetCleanlyMapped() {
    then(this.props.getInstances().size()).isEqualTo(2);
    then(this.props.getInstances().get("service1").size()).isEqualTo(2);
    then(this.props.getInstances().get("service1").get(0).getHost()).isEqualTo("s11");
    then(this.props.getInstances().get("service1").get(0).getPort()).isEqualTo(8080);
    then(this.props.getInstances().get("service1").get(0).isSecure()).isEqualTo(false);

    then(this.props.getInstances().get("service2").size()).isEqualTo(2);
    then(this.props.getInstances().get("service2").get(0).getHost()).isEqualTo("s21");
    then(this.props.getInstances().get("service2").get(0).getPort()).isEqualTo(8080);
    then(this.props.getInstances().get("service2").get(0).isSecure()).isEqualTo(true);
  }

  @Test
  void testDiscoveryClientShouldResolveSimpleValues() {
    then(this.discoveryClient.getDescription()).isEqualTo("Simple Discovery Client");
    then(this.discoveryClient.getInstances("service1")).hasSize(2);

    ServiceInstance s1 = this.discoveryClient.getInstances("service1").get(0);
    then(s1.getHost()).isEqualTo("s11");
    then(s1.getPort()).isEqualTo(8080);
    then(s1.isSecure()).isEqualTo(false);
  }

  @Test
  void testGetServices() {
    then(this.discoveryClient.getServices()).containsExactlyInAnyOrder("service1", "service2");
  }

  @Test
  void testGetANonExistentServiceShouldReturnAnEmptyList() {
    then(this.discoveryClient.getInstances("nonexistent")).isNotNull();
    then(this.discoveryClient.getInstances("nonexistent")).isEmpty();
  }

  @Configuration(proxyBeanMethods = false)
  @EnableAutoConfiguration
  public static class SampleConfig {

  }

}
