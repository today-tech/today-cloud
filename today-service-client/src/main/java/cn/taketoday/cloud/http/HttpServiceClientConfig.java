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

package cn.taketoday.cloud.http;

import cn.taketoday.cloud.RpcResponse;
import cn.taketoday.cloud.core.serialize.JdkSerialization;
import cn.taketoday.cloud.core.serialize.Serialization;
import cn.taketoday.cloud.protocol.http.HttpServiceRegistry;
import cn.taketoday.cloud.protocol.tcp.TcpServiceMethodInvoker;
import cn.taketoday.cloud.registry.RegistryProperties;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.properties.EnableConfigurationProperties;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/9/5 09:56
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RegistryProperties.class)
public class HttpServiceClientConfig {

  @MissingBean
  static HttpServiceRegistry httpServiceRegistry(RegistryProperties properties, Serialization<RpcResponse> serialization) {
    return HttpServiceRegistry.ofURL(properties.getHttpUrl(), serialization, new TcpServiceMethodInvoker(serialization));
  }

  @MissingBean
  static Serialization requestSerialization() {
    return new JdkSerialization<>();
  }

}
