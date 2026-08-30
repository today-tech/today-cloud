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

package infra.config;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.config.DependencyDescriptor;
import infra.beans.factory.support.DependencyResolvingStrategy;
import infra.context.BootstrapContext;

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
