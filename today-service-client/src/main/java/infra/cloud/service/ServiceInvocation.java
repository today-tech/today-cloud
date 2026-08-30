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
   * Service ID
   */
  String getServiceId();

  /**
   * Service metadata
   */
  ServiceMetadata getServiceMetadata();

  /**
   * service method metadata
   */
  ServiceInterfaceMethod getServiceMethod();

  /**
   * Get the arguments as an array object. It is possible to change element values
   * within this array to change the arguments.
   *
   * @return the argument of the invocation
   */
  Object[] getArguments();

  InvocationType getType();

}
