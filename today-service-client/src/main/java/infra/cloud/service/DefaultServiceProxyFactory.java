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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.ReflectionUtils;
import io.netty.buffer.ByteBufAllocator;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 2021/7/4 22:58
 */
public class DefaultServiceProxyFactory implements ServiceProxyFactory {

  private final ClientInterceptor[] interceptors;

  private final RemotingOperationsProvider remotingOperationsProvider;

  private final ServiceInterfaceMetadataProvider<ServiceInterfaceMethod> metadataProvider;

  private final ByteBufAllocator allocator = ByteBufAllocator.DEFAULT;

  public DefaultServiceProxyFactory(RemotingOperationsProvider remotingOperationsProvider,
          ServiceInterfaceMetadataProvider<ServiceInterfaceMethod> metadataProvider, List<ClientInterceptor> interceptors) {
    this.metadataProvider = metadataProvider;
    this.remotingOperationsProvider = remotingOperationsProvider;
    this.interceptors = interceptors.toArray(new ClientInterceptor[0]);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <S> S getService(Class<S> serviceInterface) {
    Assert.isTrue(serviceInterface.isInterface(), "service must be an interface");
    var metadata = metadataProvider.getMetadata(serviceInterface);
    ServiceInvoker serviceInvoker = new ServiceMethodInvoker(metadata, interceptors, remotingOperationsProvider, allocator);
    List<ServiceInterfaceMethod> serviceMethods = metadata.getServiceMethods();

    return (S) Proxy.newProxyInstance(serviceInterface.getClassLoader(), new Class[] { serviceInterface },
            new ServiceInvocationHandler(serviceInterface, serviceMethods, serviceInvoker));
  }

  static final class ServiceInvocationHandler implements InvocationHandler {

    private final Class<?> serviceInterface;

    private final ServiceInvoker serviceInvoker;

    private final Map<Method, ServiceInterfaceMethod> serviceMethods;

    public ServiceInvocationHandler(Class<?> serviceInterface, List<ServiceInterfaceMethod> methods, ServiceInvoker serviceInvoker) {
      this.serviceInterface = serviceInterface;
      this.serviceInvoker = serviceInvoker;
      this.serviceMethods = methods.stream()
              .collect(Collectors.toMap(ServiceInterfaceMethod::getMethod, Function.identity()));
    }

    @Nullable
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      ServiceInterfaceMethod serviceMethod = serviceMethods.get(method);
      if (serviceMethod != null) {
        var result = serviceInvoker.invoke(serviceMethod, args);
        return serviceMethod.resolveResult(result);
      }

      if (method.isDefault()) {
        return InvocationHandler.invokeDefault(proxy, method, args);
      }

      if (ReflectionUtils.isEqualsMethod(method)) {
        // Only consider equal when proxies are identical.
        return (proxy == args[0]);
      }
      else if (ReflectionUtils.isHashCodeMethod(method)) {
        // Use hashCode of service proxy.
        return System.identityHashCode(proxy);
      }
      else if (ReflectionUtils.isToStringMethod(method)) {
        return "Remote service proxy: " + serviceInterface;
      }

      throw new IllegalStateException("Unexpected method invocation: " + method);
    }
  }

}
