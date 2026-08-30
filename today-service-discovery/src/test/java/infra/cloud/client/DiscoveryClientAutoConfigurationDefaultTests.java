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
    then(this.discoveryClient).isInstanceOf(SimpleDiscoveryClient.class);
  }

  @EnableAutoConfiguration
  @Configuration(proxyBeanMethods = false)
  public static class Config {

  }

}
