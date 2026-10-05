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
import org.reactivestreams.Publisher;

import java.lang.reflect.Method;
import java.util.List;

import infra.cloud.service.ServiceInterfaceMetadata;
import infra.cloud.service.ServiceMethod;
import infra.core.ReactiveAdapterRegistry;
import infra.reflect.MethodInvoker;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/12/20 21:49
 */
public class InvocableMethod extends ServiceMethod {

  private final Object instance;

  private final ServiceObject serviceObject;

  private final MethodInvoker invoker;

  private final ReturnValueHandler returnValueHandler;

  public InvocableMethod(ServiceInterfaceMetadata<?> metadata, ServiceObject service, Method method, MethodInvoker invoker) {
    this(metadata, service, method, invoker, ReactiveAdapterRegistry.getSharedInstance());
  }

  public InvocableMethod(ServiceInterfaceMetadata<?> metadata, ServiceObject service, Method method, MethodInvoker invoker,
          ReactiveAdapterRegistry adapterRegistry) {
    this(metadata, service, method, invoker, adapterRegistry, new ReturnValueHandlerComposite(List.of()));
  }

  public InvocableMethod(ServiceInterfaceMetadata<?> metadata, ServiceObject service, Method method, MethodInvoker invoker,
          ReactiveAdapterRegistry adapterRegistry, ReturnValueHandlerComposite handlers) {
    super(metadata.getServiceMetadata(), service.getInterface(), method, adapterRegistry);
    this.invoker = invoker;
    this.instance = service.getInstance();
    this.serviceObject = service;
    this.returnValueHandler = handlers.select(this);
  }

  public ReturnValueHandler getReturnValueHandler() {
    return returnValueHandler;
  }

  public Publisher<Object> handleReturnValue(RemoteRequest request, @Nullable Object returnValue) {
    return returnValueHandler.handleReturnValue(request, returnValue);
  }

  public ServiceObject getServiceObject() {
    return serviceObject;
  }

  @Nullable
  public Object invoke(@Nullable Object @Nullable [] args) {
    return invoker.invoke(instance, args);
  }

}
