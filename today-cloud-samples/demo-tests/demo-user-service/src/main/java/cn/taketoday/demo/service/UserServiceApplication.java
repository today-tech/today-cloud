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

package cn.taketoday.demo.service;

import infra.app.ApplicationType;
import infra.app.InfraApplication;
import infra.app.builder.ApplicationBuilder;
import infra.cloud.provider.EnableServiceProvider;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/10/18 00:04
 */
@InfraApplication
@EnableServiceProvider
public class UserServiceApplication {

  public static void main(String[] args) {
    ApplicationBuilder.forSources(UserServiceApplication.class)
            .type(ApplicationType.NETTY_WEB)
            .run(args);
  }

}
