/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import cn.taketoday.cloud.RpcResponse;
import cn.taketoday.cloud.core.HttpRuntimeException;
import cn.taketoday.cloud.core.HttpUtils;
import cn.taketoday.cloud.core.ObjectMapperUtils;
import cn.taketoday.cloud.core.serialize.JdkSerialization;
import cn.taketoday.cloud.core.serialize.Serialization;
import cn.taketoday.cloud.JdkServiceProxy;
import cn.taketoday.cloud.registry.RegisteredStatus;
import cn.taketoday.cloud.registry.ServiceDefinition;
import cn.taketoday.cloud.registry.ServiceNotFoundException;
import cn.taketoday.cloud.ServiceProxy;
import cn.taketoday.cloud.registry.ServiceRegisterFailedException;
import cn.taketoday.cloud.registry.ServiceRegistry;
import cn.taketoday.lang.Assert;

/**
 * @author TODAY 2021/7/3 23:48
 */
public class HttpServiceRegistry implements ServiceRegistry {

  private static final TypeReference<List<ServiceDefinition>> reference = new TypeReference<List<ServiceDefinition>>() { };

  private String registryURL;
  private ServiceProxy serviceProxy;

  private Serialization<RpcResponse> serialization = new JdkSerialization<>();

  public HttpServiceRegistry() { }

  public HttpServiceRegistry(String registryURL) {
    this.registryURL = registryURL;
  }

  public void setRegistryURL(String registryURL) {
    this.registryURL = registryURL;
  }

  public String getRegistryURL() {
    return registryURL;
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

  public void setSerialization(Serialization<RpcResponse> serialization) {
    Assert.notNull(serialization, "serialization is required");
    this.serialization = serialization;
  }

  public Serialization<RpcResponse> getSerialization() {
    return serialization;
  }

  @Override
  public void register(ServiceDefinition definition) {
    register(Collections.singletonList(definition));
  }

  @Override
  public void register(List<ServiceDefinition> definitions) {
    RegisteredStatus status = HttpUtils.post(registryURL, definitions, RegisteredStatus.class);
    if (status.registeredCount != definitions.size()) {
      throw new ServiceRegisterFailedException(definitions);
    }
  }

  @Override
  public void unregister(List<ServiceDefinition> definitions) {
    HttpUtils.delete(registryURL, definitions);
  }

  /**
   * lookup for a target service
   *
   * @param serviceInterface target service interface
   * @param <T> service type
   * @return target service interface
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> T lookup(Class<T> serviceInterface) {
    final class ServiceSupplier implements Supplier<List<ServiceDefinition>> {
      @Override
      public List<ServiceDefinition> get() {
        try {
          final String json = HttpUtils.get(buildGetServiceDefinitionURL(serviceInterface));
          return ObjectMapperUtils.fromJSON(json, reference);
        }
        catch (HttpRuntimeException e) {
          throw new ServiceNotFoundException("Cannot found a service: " + serviceInterface, e);
        }
      }
    }

    Assert.state(serialization != null, "No serialization settings");
    final HttpServiceMethodInvoker methodInvoker = new HttpServiceMethodInvoker(serialization);
    return (T) getServiceProxy().getProxy(serviceInterface, new ServiceSupplier(), methodInvoker);
  }

  private <T> String buildGetServiceDefinitionURL(Class<T> serviceInterface) {
    return registryURL + '/' + serviceInterface.getName();
  }

  @Override
  public String toString() {
    return "HttpServiceRegistry{" +
            "registryURL='" + registryURL + '\'' +
            ", serviceProxy=" + serviceProxy +
            '}';
  }

  // static

  public static HttpServiceRegistry ofURL(String registryURL) {
    return new HttpServiceRegistry(registryURL);
  }

}
