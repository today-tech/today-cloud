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

package infra.cloud.protocol.http;

import java.util.ArrayList;
import java.util.List;

import infra.cloud.DiscoveryClient;
import infra.cloud.JdkServiceProxy;
import infra.cloud.RpcResponse;
import infra.cloud.ServiceInstance;
import infra.cloud.ServiceMethodInvoker;
import infra.cloud.ServiceProvider;
import infra.cloud.ServiceProxy;
import infra.cloud.core.serialize.JdkSerialization;
import infra.cloud.core.serialize.Serialization;
import infra.cloud.registry.HttpRegistration;
import infra.cloud.registry.ServiceRegisterFailedException;
import infra.cloud.registry.ServiceRegistry;
import infra.core.style.ToStringBuilder;
import infra.web.client.RestClientException;

/**
 * @author TODAY 2021/7/3 23:48
 */
public class HttpServiceRegistry implements ServiceRegistry<HttpRegistration>, ServiceProvider, DiscoveryClient {

  private ServiceProxy serviceProxy;

  private final HttpOperations httpOperations;

  private final ServiceMethodInvoker methodInvoker;

  public HttpServiceRegistry(String registryURL) {
    this.httpOperations = new HttpOperations(registryURL, new JdkSerialization<>());
    this.methodInvoker = new HttpServiceMethodInvoker(httpOperations);
  }

  HttpServiceRegistry(HttpOperations httpOperations) {
    this.httpOperations = httpOperations;
    this.methodInvoker = new HttpServiceMethodInvoker(httpOperations);
  }

  HttpServiceRegistry(HttpOperations httpOperations, ServiceMethodInvoker methodInvoker) {
    this.httpOperations = httpOperations;
    this.methodInvoker = methodInvoker;
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
  public void register(HttpRegistration registration) {
    try {
      httpOperations.register(registration);
    }
    catch (RestClientException e) {
      throw new ServiceRegisterFailedException(registration, e);
    }
  }

  @Override
  public void unregister(HttpRegistration registration) {
    httpOperations.delete(registration);
  }

  @Override
  public List<String> getServices() {
    return new ArrayList<>(httpOperations.getServices().keySet());
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<ServiceInstance> getInstances(String serviceId) {
    return httpOperations.getInstances(serviceId);
  }

  /**
   * lookup for a target service
   *
   * @param serviceInterface target service interface
   * @param <T> service type
   * @return target service interface
   */
  @Override
  public <T> T getService(Class<T> serviceInterface) {
    return getServiceProxy().getProxy(serviceInterface, this, methodInvoker);
  }

  @Override
  public String toString() {
    return ToStringBuilder.forInstance(this)
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

  public static HttpServiceRegistry ofURL(String registryURL, Serialization<RpcResponse> serialization, ServiceMethodInvoker methodInvoker) {
    return new HttpServiceRegistry(new HttpOperations(registryURL, serialization), methodInvoker);
  }

}
