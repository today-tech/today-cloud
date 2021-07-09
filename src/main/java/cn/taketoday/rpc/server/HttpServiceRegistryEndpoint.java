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

import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.DefaultMultiValueMap;
import cn.taketoday.context.utils.MultiValueMap;
import cn.taketoday.rpc.registry.ServiceDefinition;
import cn.taketoday.web.annotation.DELETE;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.POST;
import cn.taketoday.web.annotation.RequestBody;

/**
 * @author TODAY 2021/7/9 23:08
 */
public class HttpServiceRegistryEndpoint {

  private final MultiValueMap<String, ServiceDefinition>
          multiValueMap = new DefaultMultiValueMap<>(new ConcurrentHashMap<>());

  @GET("/{name}")
  public List<ServiceDefinition> lookup(String name) {
    final List<ServiceDefinition> serviceDefinitions = multiValueMap.get(name);
    if (CollectionUtils.isEmpty(serviceDefinitions)) {
      throw new IllegalStateException("cannot found a service: " + name);
    }
    // select one
    return serviceDefinitions;
  }

  @POST
  public void register(@RequestBody ServiceDefinition definition) {
    multiValueMap.add(definition.getName(), definition);
  }

  @DELETE
  public void unregister(@RequestBody ServiceDefinition definition) {
    final List<ServiceDefinition> serviceDefinitions = multiValueMap.get(definition.getName());
    if (!CollectionUtils.isEmpty(serviceDefinitions)) {
      serviceDefinitions.removeIf(def -> Objects.equals(def, definition));
    }
  }

}
