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

package infra.cloud.http;

import java.util.List;
import java.util.concurrent.Executor;

import infra.beans.factory.ObjectProvider;
import infra.beans.factory.annotation.Qualifier;
import infra.cloud.core.serialize.JdkSerialization;
import infra.cloud.core.serialize.Serialization;
import infra.cloud.protocol.EventHandler;
import infra.cloud.protocol.EventHandlers;
import infra.cloud.protocol.http.HttpServiceRegistry;
import infra.cloud.protocol.tcp.ClientResponseHandler;
import infra.cloud.protocol.tcp.TcpServiceMethodInvoker;
import infra.cloud.registry.RegistryProperties;
import infra.cloud.serialize.RpcArgumentSerialization;
import infra.cloud.serialize.RpcRequestSerialization;
import infra.cloud.serialize.RpcResponseSerialization;
import infra.context.annotation.Configuration;
import infra.context.annotation.MissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.lang.TodayStrategies;
import infra.stereotype.Component;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/9/5 09:56
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RegistryProperties.class)
public class HttpServiceClientConfig {

  @MissingBean
  static HttpServiceRegistry httpServiceRegistry(RegistryProperties properties,
          RpcRequestSerialization serialization, EventHandlers eventHandlers, Serialization requestSerialization) {
    return HttpServiceRegistry.ofURL(properties.getHttpUrl(), requestSerialization,
            new TcpServiceMethodInvoker(serialization, eventHandlers));
  }

  @Component
  @SuppressWarnings({ "rawtypes" })
  static RpcRequestSerialization rpcRequestSerialization(ObjectProvider<RpcArgumentSerialization> serializations) {
    List<RpcArgumentSerialization> list = TodayStrategies.find(RpcArgumentSerialization.class);
    serializations.addOrderedTo(list);
    return new RpcRequestSerialization(list);
  }

  @Component
  static RpcResponseSerialization responseSerialization() {
    return new RpcResponseSerialization();
  }

  @Component
  static EventHandlers eventHandlers(
          @Qualifier("applicationTaskExecutor") Executor eventAsyncExecutor, List<EventHandler> handlers) {
    // TODO eventAsyncExecutor
    return new EventHandlers(eventAsyncExecutor, handlers);
  }

  @Component
  static ClientResponseHandler clientResponseHandler(RpcResponseSerialization responseSerialization) {
    return new ClientResponseHandler(responseSerialization);
  }

  @MissingBean
  static Serialization requestSerialization() {
    return new JdkSerialization<>();
  }

}
