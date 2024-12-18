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

import infra.beans.factory.annotation.Qualifier;
import infra.cloud.RpcResponse;
import infra.cloud.core.serialize.JdkSerialization;
import infra.cloud.core.serialize.Serialization;
import infra.cloud.protocol.EventHandler;
import infra.cloud.protocol.EventHandlers;
import infra.cloud.protocol.http.HttpServiceRegistry;
import infra.cloud.protocol.tcp.ClientResponseHandler;
import infra.cloud.protocol.tcp.TcpServiceMethodInvoker;
import infra.cloud.registry.RegistryProperties;
import infra.context.annotation.Configuration;
import infra.context.annotation.MissingBean;
import infra.context.properties.EnableConfigurationProperties;
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
          Serialization<RpcResponse> serialization, EventHandlers eventHandlers) {
    return HttpServiceRegistry.ofURL(properties.getHttpUrl(), serialization,
            new TcpServiceMethodInvoker(serialization, eventHandlers));
  }

  @Component
  static EventHandlers eventHandlers(
          @Qualifier("applicationTaskExecutor") Executor eventAsyncExecutor, List<EventHandler> handlers) {
    // TODO eventAsyncExecutor
    return new EventHandlers(eventAsyncExecutor, handlers);
  }

  @Component
  static ClientResponseHandler clientResponseHandler() {
    return new ClientResponseHandler();
  }

  @MissingBean
  static Serialization requestSerialization() {
    return new JdkSerialization<>();
  }

}
