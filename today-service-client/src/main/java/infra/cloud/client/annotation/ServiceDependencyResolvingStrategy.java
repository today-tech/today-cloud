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

package infra.cloud.client.annotation;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.config.DependencyDescriptor;
import infra.beans.factory.support.DependencyResolvingStrategy;
import infra.cloud.service.ServiceProvider;
import infra.context.ApplicationContext;
import infra.stereotype.Service;

/**
 * for ServiceProvider
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/11/18 23:21
 */
public class ServiceDependencyResolvingStrategy implements DependencyResolvingStrategy {

  private ServiceProvider serviceProvider;

  private final ApplicationContext context;

  public ServiceDependencyResolvingStrategy(ApplicationContext context) {
    this.context = context;
  }

  @Nullable
  @Override
  public Object resolveDependency(DependencyDescriptor descriptor, Context context) {
    Class<?> dependencyType = descriptor.getDependencyType();
    if (dependencyType.isInterface()) {
      if (dependencyType.isAnnotationPresent(Service.class)
              || descriptor.getAnnotation(ServiceReference.class) != null) {
        if (serviceProvider == null) {
          serviceProvider = this.context.getBean(ServiceProvider.class);
        }
        if (serviceProvider != null) {
          return serviceProvider.getService(dependencyType);
        }
      }
    }
    return null;
  }

}
