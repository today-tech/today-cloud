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

package infra.cloud.provider;

import infra.context.annotation.Configuration;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.stereotype.Component;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/11/28 20:51
 */
@Configuration(proxyBeanMethods = false)
class ServicePublishConfig {

  private static final Logger log = LoggerFactory.getLogger(ServicePublishConfig.class);

  @Component
  static LocalServiceHolder localServiceHolder() {
    return new LocalServiceHolder();
  }

}
