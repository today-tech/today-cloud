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

package cn.taketoday.cloud.protocol.http;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import cn.taketoday.cloud.JdkServiceProxy;
import cn.taketoday.cloud.RpcResponse;
import cn.taketoday.cloud.ServiceProvider;
import cn.taketoday.cloud.ServiceProxy;
import cn.taketoday.cloud.core.serialize.JdkSerialization;
import cn.taketoday.cloud.core.serialize.Serialization;
import cn.taketoday.cloud.registry.RegisteredStatus;
import cn.taketoday.cloud.registry.ServiceDefinition;
import cn.taketoday.cloud.registry.ServiceRegisterFailedException;
import cn.taketoday.cloud.registry.ServiceRegistry;
import cn.taketoday.core.style.ToStringBuilder;

/**
 * @author TODAY 2021/7/3 23:48
 */
public class HttpServiceRegistry implements ServiceRegistry, ServiceProvider {

  private ServiceProxy serviceProxy;

  private final HttpOperations httpOperations;
  private final HttpServiceMethodInvoker methodInvoker;

  public HttpServiceRegistry(String registryURL) {
    this.httpOperations = new HttpOperations(registryURL, new JdkSerialization<>());
    this.methodInvoker = new HttpServiceMethodInvoker(httpOperations);
  }

  public HttpServiceRegistry(HttpOperations httpOperations) {
    this.httpOperations = httpOperations;
    this.methodInvoker = new HttpServiceMethodInvoker(httpOperations);
  }

  public void setServiceProxy(ServiceProxy serviceProxy) {
    this.serviceProxy = serviceProxy;
  }

  public ServiceProxy getServiceProxy() {
    if (serviceProxy == null) {
      serviceProxy = createServiceProxy();
    }
    return serviceProxy;
  }

  protected JdkServiceProxy createServiceProxy() {
    return new JdkServiceProxy();
  }

  @Override
  public RegisteredStatus register(ServiceDefinition definition) {
    return register(Collections.singletonList(definition));
  }

  @Override
  public RegisteredStatus register(List<ServiceDefinition> definitions) {
    RegisteredStatus status = httpOperations.register(definitions, RegisteredStatus.class);
    if (status.registeredCount != definitions.size()) {
      throw new ServiceRegisterFailedException(definitions);
    }
    return status;
  }

  @Override
  public void unregister(List<ServiceDefinition> definitions) {
    httpOperations.delete(definitions);
  }

  @Override
  public List<ServiceDefinition> lookup(String name) {
    return httpOperations.getServiceDefinitions(name);
  }

  @Override
  public List<ServiceDefinition> lookup(Class<?> serviceInterface) {
    return lookup(serviceInterface.getName());
  }

  /**
   * lookup for a target service
   *
   * @param serviceInterface target service interface
   * @param <T> service type
   * @return target service interface
   */
  @Override
  public <T> T lookupService(Class<T> serviceInterface) {
    final class ServiceSupplier implements Supplier<List<ServiceDefinition>> {
      @Override
      public List<ServiceDefinition> get() {
        return lookup(serviceInterface);
      }
    }
    return getServiceProxy().getProxy(serviceInterface, new ServiceSupplier(), methodInvoker);
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("serviceProxy", serviceProxy)
            .append("httpOperations", httpOperations)
            .toString();
  }

  // static

  public static HttpServiceRegistry ofURL(String registryURL) {
    return new HttpServiceRegistry(registryURL);
  }

  public static HttpServiceRegistry ofURL(String registryURL, Serialization<RpcResponse> serialization) {
    return new HttpServiceRegistry(new HttpOperations(registryURL, serialization));
  }

}
