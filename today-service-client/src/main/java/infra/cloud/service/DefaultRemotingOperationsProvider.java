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
            .roundRobinLoadBalanceStrategy()
            .build());
  }

  @Override
  public List<LoadBalanceTarget> apply(List<ServiceInstance> serviceInstances) {
    var targets = new ArrayList<LoadBalanceTarget>(serviceInstances.size());
    for (ServiceInstance instance : serviceInstances) {
      targets.add(LoadBalanceTarget.of(instance.getInstanceId(),
              TcpClientTransport.create(instance.getHost(), instance.getPort())));
    }
    return targets;
  }

}
