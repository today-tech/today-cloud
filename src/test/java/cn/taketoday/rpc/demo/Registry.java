/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2021 All Rights Reserved.
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

package cn.taketoday.rpc.demo;

import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.framework.WebApplication;
import cn.taketoday.rpc.registry.ServiceDefinition;
import cn.taketoday.web.annotation.DELETE;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.POST;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestMapping;

/**
 * @author TODAY 2021/7/3 23:22
 */
public class Registry {

  public static void main(String[] args) {
    WebApplication.run(Registry.class, args);
  }

  @Configuration
  @RequestMapping("/services")
  static class AppConfig {
    final ConcurrentHashMap<String, ServiceDefinition> map = new ConcurrentHashMap<>();

    @GET("/{name}")
    public ServiceDefinition lookup(String name) {
      final ServiceDefinition def = map.get(name);
      if (def == null) {
        throw new IllegalStateException("cannot found a service: " + name);
      }
      return def;
    }

    @POST
    public void register(@RequestBody ServiceDefinition definition) {
      map.put(definition.getName(), definition);
    }

    @DELETE
    public void unregister(@RequestBody ServiceDefinition definition) {
      map.put(definition.getName(), definition);
    }

  }

}
