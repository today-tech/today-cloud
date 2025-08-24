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

package infra.cloud.registry.annotation.config;

import infra.beans.factory.annotation.DisableDependencyInjection;
import infra.cloud.registry.AutoServiceRegistration;
import infra.cloud.registry.AutoServiceRegistrationProperties;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.condition.ConditionalOnProperty;
import infra.lang.Nullable;

/**
 * @author Spencer Gibb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
@DisableDependencyInjection
@Configuration(proxyBeanMethods = false)
@Import(AutoServiceRegistrationConfiguration.class)
@ConditionalOnProperty(value = "infra.cloud.service-registry.auto-registration.enabled", matchIfMissing = true)
public class AutoServiceRegistrationAutoConfiguration {

  public AutoServiceRegistrationAutoConfiguration(@Nullable AutoServiceRegistration autoRegistration, AutoServiceRegistrationProperties properties) {
    if (autoRegistration == null && properties.isFailFast()) {
      throw new IllegalStateException(
              "Auto Service Registration has been requested, but there is no AutoServiceRegistration bean");
    }
  }

}
