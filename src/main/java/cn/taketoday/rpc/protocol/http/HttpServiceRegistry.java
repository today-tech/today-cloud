/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.rpc.protocol.http;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.function.Supplier;

import cn.taketoday.rpc.ServiceRegistry;
import cn.taketoday.rpc.registry.JdkServiceProxy;
import cn.taketoday.rpc.registry.ServiceDefinition;
import cn.taketoday.rpc.registry.ServiceProxy;
import cn.taketoday.rpc.server.ServiceNotFoundException;
import cn.taketoday.rpc.utils.HttpRuntimeException;
import cn.taketoday.rpc.utils.HttpUtils;
import cn.taketoday.rpc.utils.ObjectMapperUtils;

/**
 * @author TODAY 2021/7/3 23:48
 */
public class HttpServiceRegistry implements ServiceRegistry {

  private static final TypeReference<List<ServiceDefinition>>
          reference = new TypeReference<List<ServiceDefinition>>() { };

  private String registryURL;
  private ServiceProxy serviceProxy;

  public HttpServiceRegistry() {}

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

  @Override
  public void register(ServiceDefinition definition) {
    final String json = toJSON(definition);
    HttpUtils.post(registryURL, json);
  }

  @Override
  public void unregister(ServiceDefinition definition) {
    // TODO
  }

  /**
   * lookup for a target service
   *
   * @param serviceInterface
   *         target service interface
   * @param <T>
   *         service type
   *
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
          return fromJSON(json);
        }
        catch (HttpRuntimeException e) {
          throw new ServiceNotFoundException("Cannot found a service: " + serviceInterface);
        }
      }
    }

    final HttpRpcMethodInvoker methodInvoker = new HttpRpcMethodInvoker();
    return (T) getServiceProxy().getProxy(serviceInterface, new ServiceSupplier(), methodInvoker);
  }

  private <T> String buildGetServiceDefinitionURL(Class<T> serviceInterface) {
    return registryURL + '/' + serviceInterface.getName();
  }

  private List<ServiceDefinition> fromJSON(String json) {
    return ObjectMapperUtils.fromJSON(json, reference);
  }

  private String toJSON(ServiceDefinition definition) {
    return ObjectMapperUtils.toJSON(definition);
  }

  // static

  public static HttpServiceRegistry ofURL(String registryURL) {
    return new HttpServiceRegistry(registryURL);
  }

}
