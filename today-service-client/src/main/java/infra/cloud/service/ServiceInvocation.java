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

package infra.cloud.service;

import infra.core.AttributeAccessor;

/**
 * This interface represents a service invocation.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/9 10:12
 */
public interface ServiceInvocation extends AttributeAccessor {

  /**
   * Proceeds to the next interceptor in the chain.
   *
   * @return see the children interfaces' proceed definition.
   * @throws Throwable if the invocation throws an exception.
   */
  InvocationResult proceed() throws Throwable;

  /**
   * Service name
   *
   */
  String getServiceName();

  /**
   * Service metadata
   */
  ServiceMetadata getServiceMetadata();

  /**
   * service method metadata
   */
  ServiceInterfaceMethod getMethod();

  /**
   * Get the arguments as an array object. It is possible to change element values
   * within this array to change the arguments.
   *
   * @return the argument of the invocation
   */
  Object[] getArguments();

  InvocationType getType();

}
