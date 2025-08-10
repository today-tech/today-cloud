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

import java.util.stream.Collectors;
import java.util.stream.Stream;

import infra.lang.Nullable;
import infra.remoting.Channel;
import infra.remoting.ChannelAcceptor;
import infra.remoting.Connection;

/**
 * Extends {@link InterceptorRegistry} with methods for building a chain of registered interceptors.
 * This is not intended for direct use by applications.
 */
public class InitializingInterceptorRegistry extends InterceptorRegistry {

  @Nullable
  public RequestInterceptor initRequesterRequestInterceptor(Channel channelRequester) {
    return CompositeRequestInterceptor.create(
            requesterRequestInterceptors
                    .stream()
                    .map(factory -> factory.apply(channelRequester))
                    .collect(Collectors.toList()));
  }

  @Nullable
  public RequestInterceptor initResponderRequestInterceptor(Channel channelResponder, RequestInterceptor... perConnectionInterceptors) {
    return CompositeRequestInterceptor.create(
            Stream.concat(Stream.of(perConnectionInterceptors), responderRequestInterceptors.stream()
                            .map(inteptorFactory -> inteptorFactory.apply(channelResponder)))
                    .collect(Collectors.toList()));
  }

  public Connection initConnection(ConnectionDecorator.Type type, Connection connection) {
    for (ConnectionDecorator interceptor : connectionDecorators) {
      connection = interceptor.decorate(type, connection);
    }
    return connection;
  }

  public Channel decorateRequester(Channel channel) {
    for (ChannelDecorator interceptor : requesterChannelDecorators) {
      channel = interceptor.decorate(channel);
    }
    return channel;
  }

  public Channel decorateResponder(Channel channel) {
    for (ChannelDecorator interceptor : responderChannelDecorators) {
      channel = interceptor.decorate(channel);
    }
    return channel;
  }

  public ChannelAcceptor decorateAcceptor(ChannelAcceptor acceptor) {
    for (ChannelAcceptorDecorator interceptor : channelAcceptorDecorators) {
      acceptor = interceptor.decorate(acceptor);
    }
    return acceptor;
  }

}
