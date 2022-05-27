/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.rpc.server;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.core.DefaultMultiValueMap;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.rpc.registry.RegisteredStatus;
import cn.taketoday.rpc.registry.ServiceDefinition;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.annotation.DELETE;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.POST;
import cn.taketoday.web.annotation.RequestBody;

/**
 * @author TODAY 2021/7/9 23:08
 */
public class HttpServiceRegistryEndpoint {
  private static final Logger log = LoggerFactory.getLogger(HttpServiceRegistryEndpoint.class);

  private final MultiValueMap<String, ServiceDefinition>
          serviceMapping = new DefaultMultiValueMap<>(new ConcurrentHashMap<>());

  @GET("/{name}")
  public List<ServiceDefinition> lookup(String name) {
    final List<ServiceDefinition> serviceDefinitions = serviceMapping.get(name);
    if (CollectionUtils.isEmpty(serviceDefinitions)) {
      throw new IllegalStateException("cannot found a service: " + name);
    }
    // select one
    return serviceDefinitions;
  }

  @POST
  public RegisteredStatus register(@RequestBody List<ServiceDefinition> definitions) {
    Logger log = HttpServiceRegistryEndpoint.log;
    MultiValueMap<String, ServiceDefinition> serviceMapping = getServiceMapping();
    for (final ServiceDefinition definition : definitions) {
      log.info("Registering service: [{}] ", definition);
      serviceMapping.add(definition.getName(), definition);
    }
    return RegisteredStatus.ofRegistered(definitions.size());
  }

  @DELETE
  public void unregister(@RequestBody List<ServiceDefinition> definitions) {
    MultiValueMap<String, ServiceDefinition> serviceMapping = getServiceMapping();
    for (final ServiceDefinition definition : definitions) {
      final List<ServiceDefinition> serviceDefinitions = serviceMapping.get(definition.getName());
      if (!CollectionUtils.isEmpty(serviceDefinitions)
              && serviceDefinitions.removeIf(def -> Objects.equals(def, definition))) {
        log.info("un-register service: [{}] ", definition);
      }
    }
  }

  public MultiValueMap<String, ServiceDefinition> getServiceMapping() {
    return serviceMapping;
  }
}
