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

import java.lang.reflect.Method;

import infra.core.annotation.AnnotatedMethod;

/**
 * Represents a method within a service, encapsulating metadata such as the service interface,
 * the underlying {@link Method}, and its parameters. This class provides access to service
 * identification, metadata, and reflection-based method details.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/10 08:20
 */
public class ServiceMethod extends AnnotatedMethod {

  protected final ServiceMetadata serviceMetadata;

  protected final Class<?> serviceInterface;

  public ServiceMethod(ServiceMetadata serviceMetadata, Class<?> serviceInterface, Method method) {
    super(method);
    this.serviceInterface = serviceInterface;
    this.serviceMetadata = serviceMetadata;
  }

  public String getServiceId() {
    return serviceMetadata.getId();
  }

  public ServiceMetadata getServiceMetadata() {
    return serviceMetadata;
  }

  public Class<?> getServiceInterface() {
    return serviceInterface;
  }

}
