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

import infra.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/27 22:11
 */
public class RemoteRequest {

  private final InvocableMethod method;

  private final Object[] args;

  private final ServiceObject serviceObject;

  public RemoteRequest(InvocableMethod method, Object[] args, ServiceObject serviceObject) {
    this.method = method;
    this.args = args;
    this.serviceObject = serviceObject;
  }

  @Nullable
  public Object invoke() throws Throwable {
    return method.invoke(args);
  }

  public InvocableMethod getMethod() {
    return method;
  }

  public ServiceObject getServiceInterface() {
    return serviceObject;
  }

}
