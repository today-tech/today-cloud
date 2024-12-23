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

import java.lang.reflect.Method;

import infra.cloud.RpcMethod;
import infra.reflect.MethodInvoker;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/12/20 21:49
 */
public class InvocableRpcMethod extends RpcMethod {

  private final MethodInvoker invoker;

  public InvocableRpcMethod(Method method, MethodInvoker invoker) {
    super(method);
    this.invoker = invoker;
  }

  public Object invokeAndHandle(Object serviceInstance, Object[] args) throws Throwable {
    return invoker.invoke(serviceInstance, args);
  }

}
