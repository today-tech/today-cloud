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
package infra.remoting.plugins;

import org.jspecify.annotations.Nullable;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import infra.remoting.Channel;
import infra.remoting.ChannelAcceptor;
import infra.remoting.Connection;

/**
 * Extends {@link InterceptorRegistry} with methods for building a chain of registered interceptors.
 * This is not intended for direct use by applications.
 */
public class InitializingInterceptorRegistry extends InterceptorRegistry {

  public @Nullable RequestInterceptor initRequesterRequestInterceptor(Channel channelRequester) {
    return CompositeRequestInterceptor.create(
            requesterRequestInterceptors
                    .stream()
                    .map(factory -> factory.apply(channelRequester))
                    .collect(Collectors.toList()));
  }

  public @Nullable RequestInterceptor initResponderRequestInterceptor(Channel channelResponder, RequestInterceptor... perConnectionInterceptors) {
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
