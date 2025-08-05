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

package infra.remoting.lb;

import org.reactivestreams.Publisher;

import java.util.List;

import infra.lang.Nullable;
import infra.remoting.Channel;
import infra.remoting.Payload;
import infra.remoting.core.ChannelConnector;
import infra.remoting.core.RemotingClient;
import infra.remoting.transport.ClientTransport;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * An implementation of {@link RemotingClient} backed by a pool of {@code Channel} instances and
 * using a {@link LoadBalanceStrategy} to select the {@code Channel} to use for a given request.
 */
public class LoadBalanceRemotingClient implements RemotingClient {

  private final ChannelPool channelPool;

  private LoadBalanceRemotingClient(ChannelPool channelPool) {
    this.channelPool = channelPool;
  }

  @Override
  public Mono<Void> onClose() {
    return channelPool.onClose();
  }

  @Override
  public boolean connect() {
    return channelPool.connect();
  }

  /**
   * Return {@code Mono} that selects a Channel from the underlying pool.
   */
  @Override
  public Mono<Channel> source() {
    return Mono.fromSupplier(channelPool::select);
  }

  @Override
  public Mono<Void> fireAndForget(Mono<Payload> payloadMono) {
    return payloadMono.flatMap(p -> channelPool.select().fireAndForget(p));
  }

  @Override
  public Mono<Payload> requestResponse(Mono<Payload> payloadMono) {
    return payloadMono.flatMap(p -> channelPool.select().requestResponse(p));
  }

  @Override
  public Flux<Payload> requestStream(Mono<Payload> payloadMono) {
    return payloadMono.flatMapMany(p -> channelPool.select().requestStream(p));
  }

  @Override
  public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
    return source().flatMapMany(channel -> channel.requestChannel(payloads));
  }

  @Override
  public Mono<Void> metadataPush(Mono<Payload> payloadMono) {
    return payloadMono.flatMap(p -> channelPool.select().metadataPush(p));
  }

  @Override
  public void dispose() {
    channelPool.dispose();
  }

  /**
   * Return a builder for a {@link LoadBalanceRemotingClient}.
   *
   * @param targetPublisher refreshes the list of load balance targets periodically
   * @return the created builder
   */
  public static Builder builder(Publisher<List<LoadBalanceTarget>> targetPublisher) {
    return new Builder(targetPublisher);
  }

  /** Builder for creating an {@link LoadBalanceRemotingClient}. */
  public static class Builder {

    private final Publisher<List<LoadBalanceTarget>> targetPublisher;

    @Nullable
    private ChannelConnector connector;

    @Nullable
    LoadBalanceStrategy loadbalanceStrategy;

    Builder(Publisher<List<LoadBalanceTarget>> targetPublisher) {
      this.targetPublisher = targetPublisher;
    }

    /**
     * Configure the "template" connector to use for connecting to load balance targets. To
     * establish a connection, the {@link LoadBalanceTarget#getTransport() ClientTransport}
     * contained in each target is passed to the connector's {@link
     * ChannelConnector#connect(ClientTransport) connect} method and thus the same connector with
     * the same settings applies to all targets.
     *
     * <p>By default, this is initialized with {@link ChannelConnector#create()}.
     *
     * @param connector the connector to use as a template
     */
    public Builder connector(ChannelConnector connector) {
      this.connector = connector;
      return this;
    }

    /**
     * Configure {@link RoundRobinLoadBalanceStrategy} as the strategy to use to select targets.
     *
     * <p>This is the strategy used by default.
     */
    public Builder roundRobinLoadBalanceStrategy() {
      this.loadbalanceStrategy = new RoundRobinLoadBalanceStrategy();
      return this;
    }

    /**
     * Configure {@link WeightedLoadBalanceStrategy} as the strategy to use to select targets.
     *
     * <p>By default, {@link RoundRobinLoadBalanceStrategy} is used.
     */
    public Builder weightedLoadBalanceStrategy() {
      this.loadbalanceStrategy = WeightedLoadBalanceStrategy.create();
      return this;
    }

    /**
     * Configure the {@link LoadBalanceStrategy} to use.
     *
     * <p>By default, {@link RoundRobinLoadBalanceStrategy} is used.
     */
    public Builder loadBalanceStrategy(LoadBalanceStrategy strategy) {
      this.loadbalanceStrategy = strategy;
      return this;
    }

    /** Build the {@link LoadBalanceRemotingClient} instance. */
    public LoadBalanceRemotingClient build() {
      final ChannelConnector connector = this.connector != null ? this.connector : ChannelConnector.create();
      final LoadBalanceStrategy strategy = loadbalanceStrategy != null
              ? loadbalanceStrategy
              : new RoundRobinLoadBalanceStrategy();

      if (strategy instanceof ClientLoadBalanceStrategy) {
        ((ClientLoadBalanceStrategy) strategy).initialize(connector);
      }

      return new LoadBalanceRemotingClient(
              new ChannelPool(connector, this.targetPublisher, strategy));
    }
  }
}
