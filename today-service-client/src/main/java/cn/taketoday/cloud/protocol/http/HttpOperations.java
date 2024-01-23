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

package cn.taketoday.cloud.protocol.http;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

import cn.taketoday.cloud.DefaultServiceInstance;
import cn.taketoday.cloud.RpcRequest;
import cn.taketoday.cloud.RpcResponse;
import cn.taketoday.cloud.ServiceInstance;
import cn.taketoday.cloud.core.serialize.Serialization;
import cn.taketoday.cloud.registry.ServiceNotFoundException;
import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.http.HttpEntity;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.concurrent.CompletableToListenableFutureAdapter;
import cn.taketoday.util.concurrent.ListenableFuture;
import cn.taketoday.web.client.HttpClientErrorException;
import cn.taketoday.web.client.RestClient;
import cn.taketoday.web.client.RestClientException;
import cn.taketoday.web.client.RestTemplate;
import cn.taketoday.web.reactive.function.BodyInserters;
import cn.taketoday.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/8/14 17:46
 */
final class HttpOperations {
  private static final ParameterizedTypeReference<List<DefaultServiceInstance>> reference = new ParameterizedTypeReference<>() { };

  private final Serialization<RpcResponse> serialization;

  private final RestTemplate restOperations = new RestTemplate();

  private final RestClient restClient = RestClient.create(restOperations);

  private final WebClient webClient = WebClient.create();

  private final String registryURL;

  private final Executor executor = ForkJoinPool.commonPool();

  public HttpOperations(String registryURL, Serialization<RpcResponse> serialization) {
    this.registryURL = registryURL;
    this.serialization = serialization;
  }

  public RpcResponse execute(ServiceInstance selected, RpcRequest rpcRequest) {
    return restOperations.execute(selected.getHttpURI(), HttpMethod.POST,
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

  public ListenableFuture<Object> executeFuture(ServiceInstance selected, RpcRequest rpcRequest) {
    Mono<Object> rpcResponseMono = webClient.post()
            .uri(selected.getHttpURI())
            .body(BodyInserters.fromOutputStream(requestBody(rpcRequest), executor))
            .exchangeToMono(clientResponse -> clientResponse.body((inputMessage, context) -> {
              return inputMessage.getBody().next().handle((body, sink) -> {
                try {
                  RpcResponse response = serialization.deserialize(body.asInputStream());
                  Throwable exception = response.getException();
                  if (exception != null) {
                    sink.error(exception);
                  }
                  else {
                    sink.next(response.getResult());
                  }
                }
                catch (ClassNotFoundException e) {
                  sink.error(new ServiceNotFoundException(e));
                }
                catch (IOException e) {
                  sink.error(e);
                }
              });
            }));

    return new CompletableToListenableFutureAdapter<>(rpcResponseMono.toFuture());
  }

  private Consumer<OutputStream> requestBody(RpcRequest rpcRequest) {
    return body -> {
      try {
        serialization.serialize(rpcRequest, body);
      }
      catch (IOException e) {
        throw ExceptionUtils.sneakyThrow(e);
      }
    };
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
