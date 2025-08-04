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
package infra.remoting.examples.tcp.loadbalancer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import infra.remoting.ChannelAcceptor;
import infra.remoting.core.RemotingClient;
import infra.remoting.core.RemotingServer;
import infra.remoting.lb.LoadBalanceRemotingClient;
import infra.remoting.lb.LoadBalanceTarget;
import infra.remoting.transport.netty.client.TcpClientTransport;
import infra.remoting.transport.netty.server.CloseableChannel;
import infra.remoting.transport.netty.server.TcpServerTransport;
import infra.remoting.util.DefaultPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class RoundRobinLoadbalancerExample {

  public static void main(String[] args) {
    CloseableChannel server1 =
            RemotingServer.create(
                            ChannelAcceptor.forRequestResponse(
                                    p -> {
                                      System.out.println("Server 1 got fnf " + p.getDataUtf8());
                                      return Mono.just(DefaultPayload.create("Server 1 response"))
                                              .delayElement(Duration.ofMillis(100));
                                    }))
                    .bindNow(TcpServerTransport.create(8080));

    CloseableChannel server2 =
            RemotingServer.create(
                            ChannelAcceptor.forRequestResponse(
                                    p -> {
                                      System.out.println("Server 2 got fnf " + p.getDataUtf8());
                                      return Mono.just(DefaultPayload.create("Server 2 response"))
                                              .delayElement(Duration.ofMillis(100));
                                    }))
                    .bindNow(TcpServerTransport.create(8081));

    CloseableChannel server3 =
            RemotingServer.create(
                            ChannelAcceptor.forRequestResponse(
                                    p -> {
                                      System.out.println("Server 3 got fnf " + p.getDataUtf8());
                                      return Mono.just(DefaultPayload.create("Server 3 response"))
                                              .delayElement(Duration.ofMillis(100));
                                    }))
                    .bindNow(TcpServerTransport.create(8082));

    LoadBalanceTarget target8080 = LoadBalanceTarget.of("8080", TcpClientTransport.create(8080));
    LoadBalanceTarget target8081 = LoadBalanceTarget.of("8081", TcpClientTransport.create(8081));
    LoadBalanceTarget target8082 = LoadBalanceTarget.of("8082", TcpClientTransport.create(8082));

    Flux<List<LoadBalanceTarget>> producer =
            Flux.interval(Duration.ofSeconds(5))
                    .log()
                    .map(
                            i -> {
                              int val = i.intValue();
                              switch (val) {
                                case 0:
                                  return Collections.emptyList();
                                case 1:
                                  return Collections.singletonList(target8080);
                                case 2:
                                  return Arrays.asList(target8080, target8081);
                                case 3:
                                  return Arrays.asList(target8080, target8082);
                                case 4:
                                  return Arrays.asList(target8081, target8082);
                                case 5:
                                  return Arrays.asList(target8080, target8081, target8082);
                                case 6:
                                  return Collections.emptyList();
                                case 7:
                                  return Collections.emptyList();
                                default:
                                  return Arrays.asList(target8080, target8081, target8082);
                              }
                            });

    RemotingClient client =
            LoadBalanceRemotingClient.builder(producer).roundRobinLoadBalanceStrategy().build();

    for (int i = 0; i < 10000; i++) {
      try {
        client.requestResponse(Mono.just(DefaultPayload.create("test" + i))).block();
      }
      catch (Throwable t) {
        // no ops
      }
    }
  }
}
