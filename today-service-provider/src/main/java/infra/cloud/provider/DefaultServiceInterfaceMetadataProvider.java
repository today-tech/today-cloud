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

package infra.cloud.provider;

import java.lang.reflect.Method;

import infra.cloud.service.AbstractServiceInterfaceMetadataProvider;
import infra.cloud.service.ServiceMetadata;
import infra.cloud.service.ServiceMetadataProvider;
import infra.cloud.service.ServiceMethod;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/20 22:12
 */
public class DefaultServiceInterfaceMetadataProvider extends AbstractServiceInterfaceMetadataProvider<ServiceMethod> {

  public DefaultServiceInterfaceMetadataProvider(ServiceMetadataProvider serviceMetadataProvider) {
    super(serviceMetadataProvider);
  }

  @Override
  protected ServiceMethod createServiceMethod(ServiceMetadata serviceMetadata, Class<?> serviceInterface, Method method) {
    return new ServiceMethod(serviceMetadata, serviceInterface, method);
  }

}
