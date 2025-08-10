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

import org.junit.jupiter.api.Test;

import infra.app.test.context.InfraTest;
import infra.beans.factory.annotation.Autowired;
import infra.cloud.client.ServiceInstance;
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
