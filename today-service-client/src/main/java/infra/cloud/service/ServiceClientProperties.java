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

package infra.cloud.service;

import java.time.Duration;

import infra.context.properties.ConfigurationProperties;
import infra.util.Assert;

/**
 * Configuration for remote service calls.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2026/10/4
 */
@ConfigurationProperties("today.service.client")
public class ServiceClientProperties {

  private Duration requestTimeout = Duration.ofSeconds(30);

  public Duration getRequestTimeout() {
    return requestTimeout;
  }

  public void setRequestTimeout(Duration requestTimeout) {
    Assert.notNull(requestTimeout, "requestTimeout is required");
    Assert.isTrue(!requestTimeout.isZero() && !requestTimeout.isNegative(), "requestTimeout must be positive");
    this.requestTimeout = requestTimeout;
  }
}
