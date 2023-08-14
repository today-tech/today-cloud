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

import java.util.List;

import cn.taketoday.cloud.RpcRequest;
import cn.taketoday.cloud.RpcResponse;
import cn.taketoday.cloud.core.serialize.Serialization;
import cn.taketoday.cloud.registry.ServiceDefinition;
import cn.taketoday.cloud.registry.ServiceNotFoundException;
import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.http.HttpEntity;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.client.JdkClientHttpRequestFactory;
import cn.taketoday.web.client.RestOperations;
import cn.taketoday.web.client.RestTemplate;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/8/14 17:46
 */
public class HttpOperations {
  private static final ParameterizedTypeReference<List<ServiceDefinition>> reference = new ParameterizedTypeReference<List<ServiceDefinition>>() { };

  private final Serialization<RpcResponse> serialization;
  private final RestTemplate restOperations = new RestTemplate();

  private final String registryURL;

  public HttpOperations(String registryURL, Serialization<RpcResponse> serialization) {
    this.serialization = serialization;
    this.registryURL = registryURL;
    restOperations.setRequestFactory(new JdkClientHttpRequestFactory());
  }

  public RpcResponse execute(String uri, HttpMethod method, RpcRequest rpcRequest) {
    return restOperations.execute(uri, method,
            request -> serialization.serialize(rpcRequest, request.getBody()),
            response -> {
              try {
                return serialization.deserialize(response.getBody());
              }
              catch (ClassNotFoundException e) {
                throw new ServiceNotFoundException(e);
              }
            });
  }

  public List<ServiceDefinition> getServiceDefinitions(String name) {
    return restOperations.exchange(buildGetServiceDefinitionURL(name), HttpMethod.GET, HttpEntity.EMPTY, reference).getBody();
  }

  private String buildGetServiceDefinitionURL(String serviceInterface) {
    return registryURL + '/' + serviceInterface;
  }

  public <T> T register(Object body, Class<T> targetClass) {
    return restOperations.postForObject(registryURL, body, targetClass);
  }

  public void delete(Object body) {
    restOperations.delete(registryURL, body);
  }

}
