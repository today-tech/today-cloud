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

package infra.cloud.client.annotation.config;

import org.junit.jupiter.api.Test;

import cn.taketoday.demo.service.UserService;
import infra.app.test.context.InfraTest;
import infra.beans.factory.annotation.Autowired;
import infra.cloud.service.ServiceProxyFactory;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.EnableAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/9 22:20
 */
@InfraTest
class ServiceClientAutoConfigurationTests {

  @Autowired
  ServiceProxyFactory serviceProxyFactory;

  @Test
  void serviceProxyFactory() {
    UserService userService = serviceProxyFactory.getService(UserService.class);
    assertThat(userService).isNotNull();
  }

  @EnableAutoConfiguration
  @Configuration(proxyBeanMethods = false)
  public static class Config {

  }

}