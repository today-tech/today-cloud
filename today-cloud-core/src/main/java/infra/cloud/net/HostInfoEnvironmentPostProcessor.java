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
