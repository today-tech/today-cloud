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

package cn.taketoday.cloud.provider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.concurrent.Executor;

import cn.taketoday.beans.factory.annotation.Qualifier;
import cn.taketoday.cloud.RpcResponse;
import cn.taketoday.cloud.core.serialize.JdkSerialization;
import cn.taketoday.cloud.core.serialize.Serialization;
import cn.taketoday.cloud.protocol.EventHandler;
import cn.taketoday.cloud.protocol.http.HttpServiceRegistry;
import cn.taketoday.cloud.registry.HttpRegistration;
import cn.taketoday.cloud.registry.RegistryProperties;
import cn.taketoday.cloud.registry.ServiceRegistry;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.stereotype.Component;
import cn.taketoday.stereotype.Singleton;

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
  static RpcEventHandler eventHandler(LocalServiceHolder serviceHolder) {
    return new RpcEventHandler(serviceHolder);
  }

  @Singleton
  static ServiceProviderChannelConnector httpServiceHandlerMapping(
          @Qualifier("applicationTaskExecutor") Executor eventAsyncExecutor, // TODO eventAsyncExecutor
          LocalServiceHolder serviceHolder, List<EventHandler> handlers) {
    ServiceProviderChannelConnector connector = new ServiceProviderChannelConnector(eventAsyncExecutor, handlers);
    connector.setPort(serviceHolder.getPort());
    return connector;
  }

}
