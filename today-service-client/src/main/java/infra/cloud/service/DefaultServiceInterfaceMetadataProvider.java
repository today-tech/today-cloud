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

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import infra.core.ReactiveAdapter;
import infra.core.ReactiveAdapterRegistry;
import infra.util.Assert;
import reactor.core.publisher.Flux;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/10 08:22
 */
public class DefaultServiceInterfaceMetadataProvider extends AbstractServiceInterfaceMetadataProvider<ServiceInterfaceMethod> {

  private final ArrayList<ReturnValueResolver> resolvers = new ArrayList<>();

  private final ReactiveAdapterRegistry adapterRegistry;

  public DefaultServiceInterfaceMetadataProvider(ServiceMetadataProvider serviceMetadataProvider,
          List<ReturnValueResolver> returnValueResolvers) {
    this(serviceMetadataProvider, returnValueResolvers, ReactiveAdapterRegistry.getSharedInstance());
  }

  public DefaultServiceInterfaceMetadataProvider(ServiceMetadataProvider serviceMetadataProvider,
          List<ReturnValueResolver> returnValueResolvers, ReactiveAdapterRegistry adapterRegistry) {
    super(serviceMetadataProvider);
    Assert.notNull(adapterRegistry, "adapterRegistry is required");
    this.adapterRegistry = adapterRegistry;
    resolvers.addAll(returnValueResolvers);

    resolvers.add(new ReactiveReturnValueResolver());
    resolvers.add(new BlockReturnValueResolver());
  }

  @Override
  protected ServiceInterfaceMethod createServiceMethod(ServiceMetadata serviceMetadata, Class<?> serviceInterface, Method method) {
    return new ServiceInterfaceMethod(serviceMetadata, serviceInterface, method, resolvers, adapterRegistry);
  }

  static class ReactiveReturnValueResolver implements ReturnValueResolver {

    @Override
    public boolean supportsMethod(ServiceInterfaceMethod method) {
      return method.getResponseAdapter() != null;
    }

    @Override
    public InvocationType getInvocationType(ServiceInterfaceMethod method) {
      ReactiveAdapter adapter = method.getResponseAdapter();
      if (!adapter.isMultiValue()) {
        return InvocationType.REQUEST_RESPONSE;
      }
      if (method.getParameters().length == 1 && method.getParameters()[0].getParameterType() == Flux.class) {
        return InvocationType.DUPLEX_STREAMING;
      }
      return InvocationType.RESPONSE_STREAMING;
    }

    @Override
    public Object resolve(ServiceInterfaceMethod method, InvocationResult result) {
      return method.getResponseAdapter().fromPublisher(InvocationResults.publisher(result));
    }

    @Override
    public boolean isBlocking() {
      return false;
    }
  }

  static class BlockReturnValueResolver implements ReturnValueResolver {

    @Override
    public boolean supportsMethod(ServiceInterfaceMethod method) {
      return true;
    }

    @Override
    public InvocationType getInvocationType(ServiceInterfaceMethod method) {
      return InvocationType.REQUEST_RESPONSE;
    }

    @Override
    public @Nullable Object resolve(ServiceInterfaceMethod method, InvocationResult result) throws Throwable {
      result.start();
      return ((SingleInvocationResult) result).value().join();
    }

    @Override
    public boolean isBlocking() {
      return true;
    }

  }

}
