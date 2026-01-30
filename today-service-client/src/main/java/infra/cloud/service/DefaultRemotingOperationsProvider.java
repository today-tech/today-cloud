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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import infra.cloud.client.DiscoveryClient;
import infra.cloud.client.ServiceInstance;
import infra.remoting.RemotingOperations;
import infra.remoting.core.RemotingClient;
import infra.remoting.lb.LoadBalanceTarget;
import infra.remoting.transport.netty.client.TcpClientTransport;
import reactor.core.publisher.Flux;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/10 22:08
 */
public class DefaultRemotingOperationsProvider implements RemotingOperationsProvider, Function<List<ServiceInstance>, List<LoadBalanceTarget>> {

  private final DiscoveryClient discoveryClient;

  private final ConcurrentHashMap<String, RemotingClient> remotingClientMap = new ConcurrentHashMap<>();

  private Duration discoveryPeriod = Duration.ofSeconds(10);

  public DefaultRemotingOperationsProvider(DiscoveryClient discoveryClient) {
    this.discoveryClient = discoveryClient;
  }

  /**
   * service discovery reload period
   */
  public void setDiscoveryPeriod(Duration discoveryPeriod) {
    this.discoveryPeriod = discoveryPeriod;
  }

  @Override
  public RemotingOperations getRemotingOperations(ServiceMethod serviceMethod) {
    return getRemotingOperations(serviceMethod.getServiceId());
  }

  public RemotingOperations getRemotingOperations(String serviceId) {
    return remotingClientMap.computeIfAbsent(serviceId, name -> RemotingClient.forLoadBalance(Flux.interval(discoveryPeriod)
                    .map(i -> discoveryClient.getInstances(name))
                    .map(this))
            .weightedLoadBalanceStrategy()
            .build());
  }

  @Override
  public List<LoadBalanceTarget> apply(List<ServiceInstance> instances) {
    var targets = new ArrayList<LoadBalanceTarget>(instances.size());
    for (ServiceInstance instance : instances) {
      targets.add(LoadBalanceTarget.of(instance.getInstanceId(),
              TcpClientTransport.create(instance.getHost(), instance.getPort())));
    }
    return targets;
  }

}
