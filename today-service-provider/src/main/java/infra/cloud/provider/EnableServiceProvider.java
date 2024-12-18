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

package infra.cloud.provider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.concurrent.Executor;

import infra.beans.factory.annotation.Qualifier;
import infra.cloud.RpcResponse;
import infra.cloud.core.serialize.JdkSerialization;
import infra.cloud.core.serialize.Serialization;
import infra.cloud.protocol.EventHandler;
import infra.cloud.protocol.EventHandlers;
import infra.cloud.protocol.http.HttpServiceRegistry;
import infra.cloud.registry.HttpRegistration;
import infra.cloud.registry.RegistryProperties;
import infra.cloud.registry.ServiceRegistry;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.annotation.MissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.stereotype.Component;
import infra.stereotype.Singleton;
import infra.web.server.ServerProperties;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/11/28 20:45
 */
@Import(TcpServiceProviderConfig.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface EnableServiceProvider {

}

@Import(ServicePublishConfig.class)
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ ServerProperties.class, RegistryProperties.class })
class TcpServiceProviderConfig {

  @MissingBean
  static ServiceRegistry<HttpRegistration> serviceRegistry(RegistryProperties properties, Serialization<RpcResponse> serialization) {
    return HttpServiceRegistry.ofURL(properties.getHttpUrl(), serialization);
  }

  @MissingBean
  static Serialization requestSerialization() {
    return new JdkSerialization<>();
  }

  @Component
  static RpcInvocationEventHandler rpcInvocationEventHandler(LocalServiceHolder serviceHolder) {
    return new RpcInvocationEventHandler(serviceHolder);
  }

  @Component
  static EventHandlers eventHandlers(@Qualifier("applicationTaskExecutor") Executor eventAsyncExecutor, List<EventHandler> handlers) {
    // TODO eventAsyncExecutor
    return new EventHandlers(eventAsyncExecutor, handlers);
  }

  @Singleton
  static ServiceProviderChannelConnector httpServiceHandlerMapping(LocalServiceHolder serviceHolder, EventHandlers eventHandlers) {
    ServiceProviderChannelConnector connector = new ServiceProviderChannelConnector(eventHandlers);
    connector.setPort(serviceHolder.getPort());
    return connector;
  }

}
