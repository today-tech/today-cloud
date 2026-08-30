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
