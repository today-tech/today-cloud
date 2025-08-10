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

package infra.cloud.registry;

import java.io.Serial;

import infra.cloud.RemotingException;
import infra.lang.Nullable;

/**
 * @author TODAY 2021/7/11 17:19
 */
public class ServiceRegisterFailedException extends RemotingException {

  @Serial
  private static final long serialVersionUID = 1L;

  private final Registration registration;

  public ServiceRegisterFailedException(Registration registration, @Nullable Throwable cause) {
    super("Service register failed", cause);
    this.registration = registration;
  }

  public Registration getRegistration() {
    return registration;
  }

}

