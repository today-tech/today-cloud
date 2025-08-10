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

package infra.cloud.net;

import java.util.LinkedHashMap;

import infra.app.Application;
import infra.app.context.config.ConfigDataEnvironmentPostProcessor;
import infra.app.env.EnvironmentPostProcessor;
import infra.context.properties.bind.Bindable;
import infra.context.properties.bind.Binder;
import infra.context.properties.source.ConfigurationPropertySources;
import infra.core.Ordered;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.MapPropertySource;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
public class HostInfoEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

  private static final int ORDER = Math.addExact(ConfigDataEnvironmentPostProcessor.ORDER, 1);

  @Override
  public int getOrder() {
    return ORDER;
  }

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, Application application) {
    HostInfo hostInfo = getFirstNonLoopbackHostInfo(environment);
    LinkedHashMap<String, Object> map = new LinkedHashMap<>();
    map.put("infra.app.hostname", hostInfo.getHostname());
    map.put("infra.app.ip-address", hostInfo.getIpAddress());
    MapPropertySource propertySource = new MapPropertySource("infraAppHostInfo", map);
    environment.getPropertySources().addLast(propertySource);
  }

  private HostInfo getFirstNonLoopbackHostInfo(ConfigurableEnvironment environment) {
    InetProperties target = new InetProperties();
    ConfigurationPropertySources.attach(environment);
    Binder.get(environment).bind(InetProperties.PREFIX, Bindable.ofInstance(target));
    return new InetService(target).findFirstNonLoopbackHostInfo();
  }

}
