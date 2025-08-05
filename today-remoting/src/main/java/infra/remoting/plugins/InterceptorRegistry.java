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

package infra.remoting.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import infra.remoting.Channel;

/**
 * Provides support for registering interceptors at the following levels:
 *
 * <ul>
 *   <li>{@link #forConnection(ConnectionDecorator)} -- transport level
 *   <li>{@link #forChannelAcceptor(ChannelAcceptorDecorator)} -- for accepting new connections
 *   <li>{@link #forRequester(ChannelDecorator)} -- for performing of requests
 *   <li>{@link #forResponder(ChannelDecorator)} -- for responding to requests
 * </ul>
 */
public class InterceptorRegistry {

  protected final ArrayList<Function<Channel, ? extends RequestInterceptor>> requesterRequestInterceptors = new ArrayList<>();

  protected final ArrayList<Function<Channel, ? extends RequestInterceptor>> responderRequestInterceptors = new ArrayList<>();

  protected final ArrayList<ChannelDecorator> requesterChannelDecorators = new ArrayList<>();

  protected final ArrayList<ChannelDecorator> responderChannelDecorators = new ArrayList<>();

  protected final ArrayList<ChannelAcceptorDecorator> channelAcceptorDecorators = new ArrayList<>();

  protected final ArrayList<ConnectionDecorator> connectionDecorators = new ArrayList<>();

  /**
   * Add an {@link RequestInterceptor} that will hook into Requester Channel requests' phases.
   *
   * @param interceptor a function which accepts an {@link Channel} and returns a new {@link
   * RequestInterceptor}
   */
  public InterceptorRegistry forRequestsInRequester(Function<Channel, ? extends RequestInterceptor> interceptor) {
    requesterRequestInterceptors.add(interceptor);
    return this;
  }

  /**
   * Add an {@link RequestInterceptor} that will hook into Requester Channel requests' phases.
   *
   * @param interceptor a function which accepts an {@link Channel} and returns a new {@link
   * RequestInterceptor}
   */
  public InterceptorRegistry forRequestsInResponder(Function<Channel, ? extends RequestInterceptor> interceptor) {
    responderRequestInterceptors.add(interceptor);
    return this;
  }

  /**
   * Add an {@link ChannelDecorator} that will decorate the Channel used for performing requests.
   */
  public InterceptorRegistry forRequester(ChannelDecorator interceptor) {
    requesterChannelDecorators.add(interceptor);
    return this;
  }

  /**
   * Variant of {@link #forRequester(ChannelDecorator)} with access to the list of existing
   * registrations.
   */
  public InterceptorRegistry forRequester(Consumer<List<ChannelDecorator>> consumer) {
    consumer.accept(requesterChannelDecorators);
    return this;
  }

  /**
   * Add an {@link ChannelDecorator} that will decorate the Channel used for responding to
   * requests.
   */
  public InterceptorRegistry forResponder(ChannelDecorator interceptor) {
    responderChannelDecorators.add(interceptor);
    return this;
  }

  /**
   * Variant of {@link #forResponder(ChannelDecorator)} with access to the list of existing
   * registrations.
   */
  public InterceptorRegistry forResponder(Consumer<List<ChannelDecorator>> consumer) {
    consumer.accept(responderChannelDecorators);
    return this;
  }

  /**
   * Add a {@link ChannelAcceptorDecorator} that will intercept the accepting of new connections.
   */
  public InterceptorRegistry forChannelAcceptor(ChannelAcceptorDecorator interceptor) {
    channelAcceptorDecorators.add(interceptor);
    return this;
  }

  /**
   * Variant of {@link #forChannelAcceptor(ChannelAcceptorDecorator)} with access to the list of
   * existing registrations.
   */
  public InterceptorRegistry forChannelAcceptor(Consumer<List<ChannelAcceptorDecorator>> consumer) {
    consumer.accept(channelAcceptorDecorators);
    return this;
  }

  /** Add a {@link ConnectionDecorator}. */
  public InterceptorRegistry forConnection(ConnectionDecorator interceptor) {
    connectionDecorators.add(interceptor);
    return this;
  }

  /**
   * Variant of {@link #forConnection(ConnectionDecorator)} with access to the list of
   * existing registrations.
   */
  public InterceptorRegistry forConnection(Consumer<List<ConnectionDecorator>> consumer) {
    consumer.accept(connectionDecorators);
    return this;
  }

}
