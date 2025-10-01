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

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;

import infra.cloud.service.ServiceInterfaceMetadata;
import infra.cloud.service.ServiceMethod;
import infra.reflect.MethodInvoker;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/12/20 21:49
 */
public class InvocableMethod extends ServiceMethod {

  private final Object instance;

  private final MethodInvoker invoker;

  public InvocableMethod(ServiceInterfaceMetadata<?> metadata, ServiceObject service, Method method, MethodInvoker invoker) {
    super(metadata.getServiceMetadata(), service.getInterface(), method);
    this.invoker = invoker;
    this.instance = service.getInstance();
  }

  @Nullable
  public Object invoke(Object[] args) {
    return invoker.invoke(instance, args);
  }

}
