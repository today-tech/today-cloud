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

package infra.cloud.registry.event;

import infra.cloud.client.Registration;
import infra.context.ApplicationEvent;

/**
 * Event to be published after the local service instance registers itself with a
 * discovery service.
 *
 * @param <T> - type of configuration
 * @author Spencer Gibb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
@SuppressWarnings("serial")
public class InstanceRegisteredEvent<T> extends ApplicationEvent {

  private final T config;

  private final Registration registration;

  /**
   * Creates a new {@link InstanceRegisteredEvent} instance.
   *
   * @param source The component that published the event (never {@code null}).
   * @param config The configuration of the instance.
   * @param registration registration
   */
  public InstanceRegisteredEvent(Object source, Registration registration, T config) {
    super(source);
    this.registration = registration;
    this.config = config;
  }

  public T getConfig() {
    return this.config;
  }

  public Registration getRegistration() {
    return registration;
  }

}
