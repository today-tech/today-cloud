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
