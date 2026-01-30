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
import java.util.ArrayList;
import java.util.List;

import infra.util.concurrent.Future;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/10 08:22
 */
public class DefaultServiceInterfaceMetadataProvider extends AbstractServiceInterfaceMetadataProvider<ServiceInterfaceMethod> {

  private final ArrayList<ReturnValueResolver> resolvers = new ArrayList<>();

  public DefaultServiceInterfaceMetadataProvider(ServiceMetadataProvider serviceMetadataProvider,
          List<ReturnValueResolver> returnValueResolvers) {
    super(serviceMetadataProvider);
    resolvers.addAll(returnValueResolvers);

    resolvers.add(new MonoReturnValueResolver());
    resolvers.add(new FluxReturnValueResolver());
    resolvers.add(new BlockReturnValueResolver());
    resolvers.add(new FutureReturnValueResolver());
  }

  @Override
  protected ServiceInterfaceMethod createServiceMethod(ServiceMetadata serviceMetadata, Class<?> serviceInterface, Method method) {
    return new ServiceInterfaceMethod(serviceMetadata, serviceInterface, method, resolvers);
  }

  static class FutureReturnValueResolver implements ReturnValueResolver {

    @Override
    public boolean supportsMethod(ServiceInterfaceMethod invocation) {
      return invocation.getMethod().getReturnType() == Future.class;
    }

    @Override
    public InvocationType getInvocationType(ServiceInterfaceMethod method) {
      return InvocationType.REQUEST_RESPONSE;
    }

    @Override
    public Object resolve(ServiceInterfaceMethod method, InvocationResult result) throws Throwable {
      return result.future();
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
    public Object resolve(ServiceInterfaceMethod method, InvocationResult result) throws Throwable {
      result.future().syncUninterruptibly();
      return result.future().getNow();
    }

    @Override
    public boolean isBlocking() {
      return true;
    }

  }

  static class MonoReturnValueResolver implements ReturnValueResolver {

    @Override
    public boolean supportsMethod(ServiceInterfaceMethod method) {
      return method.getMethod().getReturnType() == Mono.class;
    }

    @Override
    public InvocationType getInvocationType(ServiceInterfaceMethod method) {
      return InvocationType.REQUEST_RESPONSE;
    }

    @Override
    public Mono<?> resolve(ServiceInterfaceMethod method, InvocationResult result) throws Throwable {
      return Mono.from(result.publisher());
    }

    @Override
    public boolean isBlocking() {
      return false;
    }

  }

  static class FluxReturnValueResolver implements ReturnValueResolver {

    @Override
    public boolean supportsMethod(ServiceInterfaceMethod method) {
      return method.getMethod().getReturnType() == Flux.class;
    }

    @Override
    public InvocationType getInvocationType(ServiceInterfaceMethod method) {
      if (method.getParameters().length == 1 && method.getParameters()[0].getParameterType() == Flux.class) {
        return InvocationType.DUPLEX_STREAMING;
      }
      return InvocationType.RESPONSE_STREAMING;
    }

    @Override
    public Flux<?> resolve(ServiceInterfaceMethod method, InvocationResult result) throws Throwable {
      return Flux.from(result.publisher());
    }

    @Override
    public boolean isBlocking() {
      return false;
    }

  }
}
