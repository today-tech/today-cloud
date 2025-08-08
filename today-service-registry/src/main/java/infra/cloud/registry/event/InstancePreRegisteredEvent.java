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

import infra.cloud.registry.Registration;
import infra.context.ApplicationEvent;

/**
 * An event to fire before a service is registered.
 *
 * @author Ryan Baxter
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
public class InstancePreRegisteredEvent extends ApplicationEvent {

  private final Registration registration;

  /**
   * Create a new pre-registration event.
   *
   * @param source the object on which the event initially occurred (never {@code null})
   * @param registration the registration meta data
   */
  public InstancePreRegisteredEvent(Object source, Registration registration) {
    super(source);
    this.registration = registration;
  }

  /**
   * Get the registration data.
   *
   * @return the registration data
   */
  public Registration getRegistration() {
    return this.registration;
  }

}
