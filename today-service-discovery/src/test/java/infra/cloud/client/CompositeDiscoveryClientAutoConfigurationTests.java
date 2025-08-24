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
import infra.cloud.client.simple.SimpleDiscoveryClient;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.EnableAutoConfiguration;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Composite Discovery Client should be the one found by default.
 *
 * @author Biju Kunjummen
 */
@InfraTest
class CompositeDiscoveryClientAutoConfigurationTests {

  @Autowired
  private DiscoveryClient discoveryClient;

  @Test
  void compositeDiscoveryClientShouldBeTheDefault() {
    then(this.discoveryClient).isInstanceOf(CompositeDiscoveryClient.class);
    CompositeDiscoveryClient compositeDiscoveryClient = (CompositeDiscoveryClient) this.discoveryClient;
    then(compositeDiscoveryClient.getDiscoveryClients()).hasSize(2);
    then(compositeDiscoveryClient.getDiscoveryClients().get(0).getDescription())
            .isEqualTo("A custom discovery client");
  }

  @Test
  void simpleDiscoveryClientShouldBeHaveTheLowestPrecedence() {
    CompositeDiscoveryClient compositeDiscoveryClient = (CompositeDiscoveryClient) this.discoveryClient;
    then(compositeDiscoveryClient.getDiscoveryClients().get(0).getDescription())
            .isEqualTo("A custom discovery client");
    then(compositeDiscoveryClient.getDiscoveryClients().get(1)).isInstanceOf(SimpleDiscoveryClient.class);
  }

  @EnableAutoConfiguration
  @Configuration(proxyBeanMethods = false)
  public static class Config {

    @Bean
    public DiscoveryClient customDiscoveryClient1() {
      return new DiscoveryClient() {

        @Override
        public String getDescription() {
          return "A custom discovery client";
        }

        @Override
        public List<ServiceInstance> getInstances(String serviceId) {
          return null;
        }

        @Override
        public List<String> getServices() {
          return null;
        }

      };
    }

  }

}
