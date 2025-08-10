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

import java.util.Collections;
import java.util.List;

import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.EnableAutoConfiguration;

import static java.util.Collections.singletonList;

/**
 * Test configuration for {@link CompositeDiscoveryClient} tests.
 *
 * @author Olga Maciaszek-Sharma
 * @author Tim Ysewyn
 */
@EnableAutoConfiguration
@Configuration(proxyBeanMethods = false)
public class CompositeDiscoveryClientTestsConfig {

  static final String DEFAULT_ORDER_DISCOVERY_CLIENT = "Default order discovery client";
  static final String CUSTOM_DISCOVERY_CLIENT = "A custom discovery client";
  static final String FOURTH_DISCOVERY_CLIENT = "Fourth discovery client";
  static final String CUSTOM_SERVICE_ID = "custom";

  @Bean
  public DiscoveryClient customDiscoveryClient() {
    return aDiscoveryClient(-1, CUSTOM_DISCOVERY_CLIENT);
  }

  @Bean
  public DiscoveryClient thirdOrderCustomDiscoveryClient() {
    return aDiscoveryClient(3, FOURTH_DISCOVERY_CLIENT);
  }

  @Bean
  public DiscoveryClient defaultOrderDiscoveryClient() {
    return aDiscoveryClient(null, DEFAULT_ORDER_DISCOVERY_CLIENT);
  }

  private DiscoveryClient aDiscoveryClient(Integer order, String description) {
    class TestDiscoveryClient implements DiscoveryClient {
      @Override
      public String getDescription() {
        return description;
      }

      @Override
      public List<ServiceInstance> getInstances(String serviceId) {
        if (serviceId.equals(CUSTOM_SERVICE_ID)) {
          ServiceInstance s1 = new DefaultServiceInstance("customInstance", CUSTOM_SERVICE_ID, "host", 123, false);
          return List.of(s1);
        }
        return Collections.emptyList();
      }

      @Override
      public List<String> getServices() {
        return singletonList(CUSTOM_SERVICE_ID);
      }

      @Override
      public int getOrder() {
        return order != null ? order : DiscoveryClient.super.getOrder();
      }

      @Override
      public String toString() {
        return description;
      }
    }
    return new TestDiscoveryClient();
  }

}
