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

package io.rsocket.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import io.rsocket.Channel;

/**
 * Provides support for registering interceptors at the following levels:
 *
 * <ul>
 *   <li>{@link #forConnection(ConnectionInterceptor)} -- transport level
 *   <li>{@link #forChannelAcceptor(ChannelAcceptorInterceptor)} -- for accepting new connections
 *   <li>{@link #forRequester(ChannelInterceptor)} -- for performing of requests
 *   <li>{@link #forResponder(ChannelInterceptor)} -- for responding to requests
 * </ul>
 */
public class InterceptorRegistry {

  protected final ArrayList<Function<Channel, ? extends RequestInterceptor>> requesterRequestInterceptors = new ArrayList<>();

  protected final ArrayList<Function<Channel, ? extends RequestInterceptor>> responderRequestInterceptors = new ArrayList<>();

  protected final ArrayList<ChannelInterceptor> requesterChannelInterceptors = new ArrayList<>();

  protected final ArrayList<ChannelInterceptor> responderChannelInterceptors = new ArrayList<>();

  protected final ArrayList<ChannelAcceptorInterceptor> channelAcceptorInterceptors = new ArrayList<>();

  protected final ArrayList<ConnectionInterceptor> connectionInterceptors = new ArrayList<>();

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
   * Add an {@link ChannelInterceptor} that will decorate the Channel used for performing requests.
   */
  public InterceptorRegistry forRequester(ChannelInterceptor interceptor) {
    requesterChannelInterceptors.add(interceptor);
    return this;
  }

  /**
   * Variant of {@link #forRequester(ChannelInterceptor)} with access to the list of existing
   * registrations.
   */
  public InterceptorRegistry forRequester(Consumer<List<ChannelInterceptor>> consumer) {
    consumer.accept(requesterChannelInterceptors);
    return this;
  }

  /**
   * Add an {@link ChannelInterceptor} that will decorate the Channel used for responding to
   * requests.
   */
  public InterceptorRegistry forResponder(ChannelInterceptor interceptor) {
    responderChannelInterceptors.add(interceptor);
    return this;
  }

  /**
   * Variant of {@link #forResponder(ChannelInterceptor)} with access to the list of existing
   * registrations.
   */
  public InterceptorRegistry forResponder(Consumer<List<ChannelInterceptor>> consumer) {
    consumer.accept(responderChannelInterceptors);
    return this;
  }

  /**
   * Add a {@link ChannelAcceptorInterceptor} that will intercept the accepting of new connections.
   */
  public InterceptorRegistry forChannelAcceptor(ChannelAcceptorInterceptor interceptor) {
    channelAcceptorInterceptors.add(interceptor);
    return this;
  }

  /**
   * Variant of {@link #forChannelAcceptor(ChannelAcceptorInterceptor)} with access to the list of
   * existing registrations.
   */
  public InterceptorRegistry forChannelAcceptor(Consumer<List<ChannelAcceptorInterceptor>> consumer) {
    consumer.accept(channelAcceptorInterceptors);
    return this;
  }

  /** Add a {@link ConnectionInterceptor}. */
  public InterceptorRegistry forConnection(ConnectionInterceptor interceptor) {
    connectionInterceptors.add(interceptor);
    return this;
  }

  /**
   * Variant of {@link #forConnection(ConnectionInterceptor)} with access to the list of
   * existing registrations.
   */
  public InterceptorRegistry forConnection(Consumer<List<ConnectionInterceptor>> consumer) {
    consumer.accept(connectionInterceptors);
    return this;
  }

}
