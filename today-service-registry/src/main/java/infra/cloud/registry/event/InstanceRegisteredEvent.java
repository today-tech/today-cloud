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

package infra.cloud.registry.event;

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

  /**
   * Creates a new {@link InstanceRegisteredEvent} instance.
   *
   * @param source The component that published the event (never {@code null}).
   * @param config The configuration of the instance.
   */
  public InstanceRegisteredEvent(Object source, T config) {
    super(source);
    this.config = config;
  }

  public T getConfig() {
    return this.config;
  }

}
