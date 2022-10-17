/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

import cn.taketoday.core.NestedRuntimeException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2021/7/9 22:40
 */
public class ServiceNotFoundException extends NestedRuntimeException {
  @Serial
  private static final long serialVersionUID = 1L;

  public ServiceNotFoundException() {
    super();
  }

  public ServiceNotFoundException(String message) {
    super(message);
  }

  public ServiceNotFoundException(Throwable cause) {
    super(cause);
  }

  public ServiceNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

}
