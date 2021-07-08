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

import java.io.IOException;

import cn.taketoday.context.utils.Assert;
import cn.taketoday.rpc.ServiceRegistry;
import cn.taketoday.rpc.registry.JdkServiceProxy;
import cn.taketoday.rpc.registry.ServiceDefinition;
import cn.taketoday.rpc.registry.ServiceProxy;
import cn.taketoday.rpc.utils.HttpUtils;
import cn.taketoday.rpc.utils.ObjectMapperUtils;

/**
 * @author TODAY 2021/7/3 23:48
 */
public class HttpServiceRegistry implements ServiceRegistry {

  private String registryURL;
  private ServiceProxy serviceProxy = new JdkServiceProxy();

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
    Assert.notNull(serviceProxy, "serviceProxy must not be null");
    this.serviceProxy = serviceProxy;
  }

  public ServiceProxy getServiceProxy() {
    return serviceProxy;
  }

  @Override
  public void register(ServiceDefinition definition) {
    final String json = toJSON(definition);
    HttpUtils.doPost(registryURL, json);
  }

  @Override
  public void unregister(ServiceDefinition definition) {
//    definition.getName()

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
    final String json = HttpUtils.doGet(registryURL + "/" + serviceInterface.getName());
    if (json == null) {
      throw new IllegalStateException("Cannot found a service: " + serviceInterface);
    }

    final ServiceDefinition serviceDefinition = fromJSON(json);
    serviceDefinition.setServiceInterface(serviceInterface);
    final HttpRpcMethodInvoker methodInvoker = new HttpRpcMethodInvoker();
    return (T) serviceProxy.getProxy(serviceDefinition, methodInvoker);
  }

  private ServiceDefinition fromJSON(String json) {
    return ObjectMapperUtils.fromJSON(json, ServiceDefinition.class);
  }

  private String toJSON(ServiceDefinition definition) {
      return ObjectMapperUtils.toJSON(definition);

  }

  // static

  public static HttpServiceRegistry ofURL(String registryURL) {
    return new HttpServiceRegistry(registryURL);
  }

}
