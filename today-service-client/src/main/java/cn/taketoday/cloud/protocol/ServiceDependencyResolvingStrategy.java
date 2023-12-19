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

package cn.taketoday.cloud.protocol;

import cn.taketoday.beans.factory.config.DependencyDescriptor;
import cn.taketoday.beans.factory.support.DependencyResolvingStrategy;
import cn.taketoday.cloud.ServiceProvider;
import cn.taketoday.cloud.client.ServiceReference;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Service;

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
