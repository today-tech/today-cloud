/*
 * Copyright 2021 - 2023 the original author or authors.
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

package cn.taketoday.config.etcd;

import cn.taketoday.context.properties.bind.BindResult;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.framework.Application;
import cn.taketoday.framework.env.EnvironmentPostProcessor;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/11/7 21:34
 */
public class ConfigApplicationStartupListener implements EnvironmentPostProcessor, Ordered {

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, Application application) {
    BindResult<EtcdProperties> result = Binder.get(environment).bind("config.server.etcd", EtcdProperties.class);
    if (result.isBound()) {
      EtcdProperties properties = result.get();
      Client client = Client.builder()
              .endpoints(properties.getEndpoints())
              .user(ByteSequenceUtils.forString(properties.getUsername()))
              .password(ByteSequenceUtils.forString(properties.getPassword()))
              .build();

      KV kvClient = client.getKVClient();
      environment.getPropertySources()
              .addLast(new EtcdPropertySource(kvClient, properties.getNamespace()));
    }
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }

}
