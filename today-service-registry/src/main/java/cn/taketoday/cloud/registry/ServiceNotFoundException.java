/*
 * Copyright 2021 - 2023 the original author or authors.
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

package cn.taketoday.cloud.registry;

import java.io.Serial;

import cn.taketoday.cloud.RemotingException;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2021/7/9 22:40
 */
public class ServiceNotFoundException extends RemotingException {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Constructs a ServiceNotFoundException
   */
  public ServiceNotFoundException() {
    super();
  }

  /**
   * Constructs a ServiceNotFoundException
   *
   * @param name service name
   */
  public ServiceNotFoundException(String name) {
    this(name, null);
  }

  /**
   * Constructs a ServiceNotFoundException
   *
   * @param serviceInterface service
   */
  public ServiceNotFoundException(Class<?> serviceInterface) {
    super(serviceInterface.getName());
  }

  public ServiceNotFoundException(@Nullable Throwable cause) {
    super(cause);
  }

  public ServiceNotFoundException(String name, @Nullable Throwable cause) {
    super("Cannot found a service: '" + name + "'", cause);
  }

}
