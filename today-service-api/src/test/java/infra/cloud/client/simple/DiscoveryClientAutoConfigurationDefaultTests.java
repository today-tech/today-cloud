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
import infra.cloud.client.CompositeDiscoveryClient;
import infra.cloud.client.DiscoveryClient;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.EnableAutoConfiguration;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * DiscoveryClient implementation defaults to {@link CompositeDiscoveryClient}.
 *
 * @author Biju Kunjummen
 */
@InfraTest(classes = DiscoveryClientAutoConfigurationDefaultTests.Config.class)
class DiscoveryClientAutoConfigurationDefaultTests {

  @Autowired
  private DiscoveryClient discoveryClient;

  @Test
  void simpleDiscoveryClientShouldBeTheDefault() {
    then(this.discoveryClient).isInstanceOf(CompositeDiscoveryClient.class);
  }

  @EnableAutoConfiguration
  @Configuration(proxyBeanMethods = false)
  public static class Config {

  }

}
