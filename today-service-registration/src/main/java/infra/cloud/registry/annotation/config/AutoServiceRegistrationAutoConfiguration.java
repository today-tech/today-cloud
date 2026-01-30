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

package infra.cloud.registry.annotation.config;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.annotation.DisableDependencyInjection;
import infra.cloud.registry.AutoServiceRegistration;
import infra.cloud.registry.AutoServiceRegistrationProperties;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.condition.ConditionalOnProperty;

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
