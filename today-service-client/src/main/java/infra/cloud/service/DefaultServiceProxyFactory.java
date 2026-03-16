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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import infra.lang.Assert;
import infra.util.ReflectionUtils;

/**
 * Default implementation of {@link ServiceProxyFactory} that creates service proxies
 * using dynamic proxy mechanisms. This factory configures proxies with the necessary
 * interceptors, remoting operations provider, and service interface metadata to handle
 * remote method invocations.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 2021/7/4 22:58
 */
public class DefaultServiceProxyFactory implements ServiceProxyFactory {

  private final ServiceInterfaceMetadataProvider<ServiceInterfaceMethod> metadataProvider;

  private final ServiceInvoker serviceInvoker;

  public DefaultServiceProxyFactory(ServiceInterfaceMetadataProvider<ServiceInterfaceMethod> metadataProvider, ServiceInvoker serviceInvoker) {
    Assert.notNull(metadataProvider, "metadataProvider is required");
    Assert.notNull(serviceInvoker, "serviceInvoker is required");
    this.metadataProvider = metadataProvider;
    this.serviceInvoker = serviceInvoker;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <S> S getService(Class<S> serviceInterface) {
    Assert.isTrue(serviceInterface.isInterface(), "service must be an interface");
    var metadata = metadataProvider.getMetadata(serviceInterface);
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

    @Override
    public @Nullable Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
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
