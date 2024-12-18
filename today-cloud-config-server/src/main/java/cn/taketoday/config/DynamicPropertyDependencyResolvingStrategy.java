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

package cn.taketoday.config;

import infra.beans.factory.config.DependencyDescriptor;
import infra.beans.factory.support.DependencyResolvingStrategy;
import infra.context.BootstrapContext;
import infra.lang.Nullable;

/**
 * for DynamicProperty
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/11/18 23:21
 */
public class DynamicPropertyDependencyResolvingStrategy implements DependencyResolvingStrategy {
  private final BootstrapContext bootstrapContext;

  public DynamicPropertyDependencyResolvingStrategy(BootstrapContext bootstrapContext) {
    this.bootstrapContext = bootstrapContext;
  }

  @Nullable
  @Override
  public Object resolveDependency(DependencyDescriptor descriptor, Context context) {
    if (descriptor.getDependencyType() == DynamicProperty.class) {
      return new DynamicProperty();
    }
    return null;
  }

}
