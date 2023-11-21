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

import java.net.URI;
import java.util.List;
import java.util.Map;

import cn.taketoday.cloud.DefaultServiceInstance;
import cn.taketoday.cloud.RpcRequest;
import cn.taketoday.cloud.RpcResponse;
import cn.taketoday.cloud.core.ServiceInstance;
import cn.taketoday.cloud.core.serialize.Serialization;
import cn.taketoday.cloud.registry.ServiceNotFoundException;
import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.http.HttpEntity;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.web.client.HttpClientErrorException;
import cn.taketoday.web.client.RestClient;
import cn.taketoday.web.client.RestClientException;
import cn.taketoday.web.client.RestTemplate;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/8/14 17:46
 */
final class HttpOperations {
  private static final ParameterizedTypeReference<List<DefaultServiceInstance>> reference = new ParameterizedTypeReference<>() { };

  private final Serialization<RpcResponse> serialization;

  private final RestTemplate restOperations = new RestTemplate();

  private final RestClient restClient = RestClient.create(restOperations);

  private final String registryURL;

  public HttpOperations(String registryURL, Serialization<RpcResponse> serialization) {
    this.registryURL = registryURL;
    this.serialization = serialization;
  }

  public RpcResponse execute(ServiceInstance selected, RpcRequest rpcRequest) {
    return execute(selected.getUri(), HttpMethod.POST, rpcRequest);
  }

  public RpcResponse execute(URI uri, HttpMethod method, RpcRequest rpcRequest) {
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

  @SuppressWarnings("rawtypes")
  public List getInstances(String name) {
    try {
      return restOperations.exchange(buildGetServiceDefinitionURL(name), HttpMethod.GET, HttpEntity.EMPTY, reference).getBody();
    }
    catch (HttpClientErrorException.NotFound e) {
      throw new ServiceNotFoundException(name, e);
    }
  }

  private String buildGetServiceDefinitionURL(String serviceInterface) {
    return registryURL + '/' + serviceInterface;
  }

  public void register(Object body) throws RestClientException {
    restClient.post()
            .uri(registryURL)
            .body(body)
            .execute();
  }

  public void delete(Object body) {
    restClient.delete()
            .uri(registryURL)
            .body(body)
            .execute();
  }

  public Map<String, Object> getServices() {
    return restClient.get()
            .uri(registryURL)
            .retrieve().body(new ParameterizedTypeReference<Map<String, Object>>() { });
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("registryURL", registryURL)
            .toString();
  }
}
