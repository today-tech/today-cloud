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
  public Object invoke(@Nullable Object @Nullable [] args) {
    return invoker.invoke(instance, args);
  }

}
